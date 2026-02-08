package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.GetScanByIdUseCase
import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanResultUiState(
    val result: BarcodeResult? = null,
    val isFavorite: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val renameInput: String = "",
    val customName: String = "Texto",
    val isLoading: Boolean = false
)

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    private val getScanByIdUseCase: GetScanByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()

    fun init(scanId: Long) {
        // If we already have the result (e.g. just scanned), we might already have it in state
        if (_uiState.value.result?.id == scanId) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getScanByIdUseCase(scanId)
            if (result != null) {
                _uiState.update { 
                    it.copy(
                        result = result, 
                        isFavorite = result.isFavorite, 
                        customName = result.customName ?: "Texto",
                        renameInput = result.customName ?: "Texto",
                        isLoading = false
                    ) 
                }
            } else {
                // Fallback or error state
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite() {
        val currentResult = _uiState.value.result ?: return
        val newFavoriteStatus = !currentResult.isFavorite
        viewModelScope.launch {
            toggleFavoriteUseCase(currentResult.id, newFavoriteStatus)
            _uiState.update { 
                it.copy(
                    isFavorite = newFavoriteStatus,
                    result = currentResult.copy(isFavorite = newFavoriteStatus)
                ) 
            }
        }
    }

    fun openRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = true) }
    }

    fun closeRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun updateRenameInput(name: String) {
        _uiState.update { it.copy(renameInput = name) }
    }

    fun saveName() {
        val currentResult = _uiState.value.result ?: return
        val newName = _uiState.value.renameInput
        viewModelScope.launch {
            updateScanNameUseCase(currentResult.id, newName)
            _uiState.update { 
                it.copy(
                    isRenameDialogOpen = false,
                    customName = newName,
                    result = currentResult.copy(customName = newName)
                ) 
            }
        }
    }
}
