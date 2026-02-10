package com.scannerpro.lectorqr.presentation.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.GetFavoritesUseCase
import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val scans: List<BarcodeResult> = emptyList(),
    val isLoading: Boolean = false,
    val isPremium: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            settingsRepository.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getFavoritesUseCase().collect { favorites ->
                _uiState.update { it.copy(scans = favorites, isLoading = false) }
            }
        }
    }

    fun toggleFavorite(scan: BarcodeResult) {
        viewModelScope.launch {
            toggleFavoriteUseCase(scan.id, !scan.isFavorite)
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            deleteScanUseCase(id)
        }
    }

    fun updateName(id: Long, newName: String) {
        viewModelScope.launch {
            updateScanNameUseCase(id, newName)
        }
    }
}
