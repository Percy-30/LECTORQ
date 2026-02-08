package com.scannerpro.lectorqr.presentation.ui.scanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IScannerRepository
import com.scannerpro.lectorqr.domain.usecase.ScanCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanCodeUseCase: ScanCodeUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val repository: IScannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _scanResultUiState = MutableStateFlow(ScanResultUiState())
    val scanResultUiState: StateFlow<ScanResultUiState> = _scanResultUiState.asStateFlow()

    init {
        observeScanResults()
    }

    private fun observeScanResults() {
        viewModelScope.launch {
            scanCodeUseCase().collect { result ->
                _uiState.update { it.copy(lastResult = result) }
                // Also update the instant overlay state
                _scanResultUiState.update { 
                    it.copy(
                        result = result,
                        isFavorite = result.isFavorite,
                        customName = result.customName ?: "Texto",
                        renameInput = result.customName ?: "Texto",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun handleBarcode(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?) {
        viewModelScope.launch {
            // This will trigger the flow emission observed in observeScanResults
            repository.onBarcodeDetected(barcode, bitmap)
        }
    }

    fun toggleFavorite() {
        val currentResult = _scanResultUiState.value.result ?: return
        val newFavoriteStatus = !currentResult.isFavorite
        viewModelScope.launch {
            toggleFavoriteUseCase(currentResult.id, newFavoriteStatus)
            _scanResultUiState.update { 
                it.copy(
                    isFavorite = newFavoriteStatus,
                    result = currentResult.copy(isFavorite = newFavoriteStatus)
                ) 
            }
        }
    }

    fun openRenameDialog() {
        _scanResultUiState.update { it.copy(isRenameDialogOpen = true) }
    }

    fun closeRenameDialog() {
        _scanResultUiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun updateRenameInput(name: String) {
        _scanResultUiState.update { it.copy(renameInput = name) }
    }

    fun saveName() {
        val currentResult = _scanResultUiState.value.result ?: return
        val newName = _scanResultUiState.value.renameInput
        viewModelScope.launch {
            updateScanNameUseCase(currentResult.id, newName)
            _scanResultUiState.update { 
                it.copy(
                    isRenameDialogOpen = false,
                    customName = newName,
                    result = currentResult.copy(customName = newName)
                ) 
            }
        }
    }

    fun scanFromGallery(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.processImageFromGallery(uri)
            if (result != null) {
                _scanResultUiState.update { 
                    it.copy(
                        result = result,
                        isFavorite = result.isFavorite,
                        customName = result.customName ?: "Texto",
                        renameInput = result.customName ?: "Texto",
                        isLoading = false
                    )
                }
            }
            _uiState.update { it.copy(lastResult = result, isLoading = false) }
        }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
    }

    fun flipCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun onZoomChanged(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
    }

    fun onResultHandled() {
        _uiState.update { it.copy(lastResult = null) }
        _scanResultUiState.update { ScanResultUiState() } // Reset overlay state
    }
}
