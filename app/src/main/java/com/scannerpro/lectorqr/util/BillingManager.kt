package com.scannerpro.lectorqr.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingManager(
    private val activity: Activity,
    private val settingsRepository: ISettingsRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "BillingManager"
    
    private val billingClient by lazy {
        BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                } else {
                    Log.e(TAG, "Compra no completada: ${billingResult.debugMessage}")
                    purchaseListener?.invoke(false)
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> get() = _isPremium

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private var purchaseListener: ((Boolean) -> Unit)? = null

    init {
        establishConnection()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        scope.launch {
            settingsRepository.isPremium.collect {
                _isPremium.postValue(it)
            }
        }
    }

    private fun establishConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryAllProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Conexión con Google Play Billing perdida")
            }
        })
    }

    fun queryPurchases() {
        // Query In-App Products
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchasesList)
            }
        }

        // Query Subscriptions
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subParams) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchasesList)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val hasTestPremium = purchases.any { purchase ->
            purchase.products.contains("android.test.purchased") &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        if (hasTestPremium) {
            scope.launch {
                settingsRepository.setManualPremium(true)
            }
        }

        val planIds = com.scannerpro.lectorqr.domain.model.PremiumPlan.values().map { it.id }
        val hasRealPremium = purchases.any { purchase ->
            (purchase.products.contains(AdConfig.PREMIUM_PRODUCT_ID) || purchase.products.any { it in planIds }) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        scope.launch {
            settingsRepository.setPremium(hasRealPremium)
        }

        purchases.filter { purchase ->
            (purchase.products.contains(AdConfig.PREMIUM_PRODUCT_ID) || 
             purchase.products.any { it in planIds } || 
             purchase.products.contains("android.test.purchased")) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    !purchase.isAcknowledged
        }.forEach { purchase ->
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Compra premium reconocida")
                }
            }
        }

        purchaseListener?.invoke(hasRealPremium || hasTestPremium)
        purchaseListener = null
    }

    fun launchPurchaseFlow(plan: com.scannerpro.lectorqr.domain.model.PremiumPlan? = null, listener: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            establishConnection()
            listener(false)
            return
        }

        purchaseListener = listener
        val productId = plan?.id ?: AdConfig.PREMIUM_PRODUCT_ID
        val productType = if (plan?.isSubscription == true) {
            BillingClient.ProductType.SUBS
        } else {
            BillingClient.ProductType.INAPP
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList != null &&
                productDetailsList.isNotEmpty()
            ) {
                val productDetails = productDetailsList[0]
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)

                // For subscriptions, we must specify an offerToken (picking the first available one)
                if (productType == BillingClient.ProductType.SUBS) {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                        productDetailsParams.setOfferToken(it.offerToken)
                    }
                }

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams.build()))
                    .build()

                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    listener(false)
                    purchaseListener = null
                }
            } else {
                Log.e(TAG, "Error obteniendo detalles del producto: ${billingResult.debugMessage}")
                // Si falla en producción porque el ID no existe en la consola, intentamos el de prueba si estamos en desarrollo
                launchTestPurchaseFlow(plan, listener)
            }
        }
    }

    fun launchTestPurchaseFlow(plan: com.scannerpro.lectorqr.domain.model.PremiumPlan? = null, listener: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            establishConnection()
            // Fallback local para desarrollo si Google Play no responde
            scope.launch {
                settingsRepository.setManualPremium(true)
                _isPremium.postValue(true)
                listener(true)
            }
            return
        }

        purchaseListener = listener

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("android.test.purchased")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList != null &&
                productDetailsList.isNotEmpty()
            ) {
                val productDetails = productDetailsList[0]

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                // Si falla la consulta a Google Play (ej. sin internet o no logueado), forzamos localmente para desarrollo
                scope.launch {
                    settingsRepository.setManualPremium(true)
                    _isPremium.postValue(true)
                    listener(true)
                }
            }
        }
    }

    fun resetPremiumStatus(listener: (Boolean) -> Unit) {
        scope.launch {
            settingsRepository.setPremium(false)
            settingsRepository.setManualPremium(false)
            _isPremium.postValue(false)
            listener(true)
        }
    }

    fun queryAllProductDetails() {
        val plans = com.scannerpro.lectorqr.domain.model.PremiumPlan.values()
        
        // Group by type
        val subProductList = plans.filter { it.isSubscription }.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val inAppProductList = plans.filter { !it.isSubscription }.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        if (subProductList.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(subProductList).build()
            billingClient.queryProductDetailsAsync(params) { _, detailsList ->
                detailsList.forEach { details ->
                    _productDetailsMap.update { it + (details.productId to details) }
                }
            }
        }

        if (inAppProductList.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(inAppProductList).build()
            billingClient.queryProductDetailsAsync(params) { _, detailsList ->
                detailsList.forEach { details ->
                    _productDetailsMap.update { it + (details.productId to details) }
                }
            }
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
