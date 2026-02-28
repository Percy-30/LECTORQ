package com.scannerpro.lectorqr.presentation.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.PremiumPlan
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import com.scannerpro.lectorqr.util.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val isManualPremium: Boolean = false,
    val selectedPlan: PremiumPlan = PremiumPlan.YEARLY,
    val dynamicPrices: Map<String, String> = emptyMap()
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private var billingManager: BillingManager? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.isPremium,
                settingsRepository.isManualPremium
            ) { isPremium, isManual ->
                _uiState.update { it.copy(
                    isPremium = isPremium,
                    isManualPremium = isManual
                ) }
            }.collect()
        }
    }

    fun setBillingManager(manager: BillingManager) {
        this.billingManager = manager
        
        // Start collecting product details for dynamic pricing
        viewModelScope.launch {
            manager.productDetailsMap
                .onEach { detailsMap ->
                    val prices = detailsMap.mapValues { entry ->
                        val details = entry.value
                        if (details.productType == BillingClient.ProductType.SUBS) {
                            details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                                ?: "N/A"
                        } else {
                            details.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
                        }
                    }
                    _uiState.update { it.copy(dynamicPrices = prices) }
                }
                .collect { }
        }
        
        // Refresh prices
        manager.queryAllProductDetails()
    }

    fun selectPlan(plan: PremiumPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    fun purchaseSelectedPlan(onResult: (Boolean) -> Unit) {
        billingManager?.launchPurchaseFlow(_uiState.value.selectedPlan) { success ->
            onResult(success)
        }
    }

    fun setManualPremium(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setManualPremium(enabled)
        }
    }

    fun forceResetAndBypass() {
        viewModelScope.launch {
            settingsRepository.setPremium(false)
            settingsRepository.setManualPremium(false)
        }
    }
}
