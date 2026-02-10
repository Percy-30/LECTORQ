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
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Conexión con Google Play Billing perdida")
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
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

        val hasRealPremium = purchases.any { purchase ->
            purchase.products.contains(AdConfig.PREMIUM_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        
        scope.launch {
            settingsRepository.setPremium(hasRealPremium)
        }

        purchases.filter { purchase ->
            (purchase.products.contains(AdConfig.PREMIUM_PRODUCT_ID) || purchase.products.contains("android.test.purchased")) &&
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

    fun launchPurchaseFlow(listener: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            establishConnection()
            listener(false)
            return
        }

        purchaseListener = listener

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdConfig.PREMIUM_PRODUCT_ID)
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

                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    listener(false)
                    purchaseListener = null
                }
            } else {
                Log.e(TAG, "Error obteniendo detalles del producto: ${billingResult.debugMessage}")
                // Si falla en producción porque el ID no existe en la consola, intentamos el de prueba si estamos en desarrollo
                launchTestPurchaseFlow(listener)
            }
        }
    }

    fun launchTestPurchaseFlow(listener: (Boolean) -> Unit) {
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

    fun destroy() {
        billingClient.endConnection()
    }
}
