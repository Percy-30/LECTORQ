package com.scannerpro.lectorqr.presentation.ui.scanner

import com.scannerpro.lectorqr.domain.model.BarcodeResult

data class ScannerUiState(
    val lastResult: BarcodeResult? = null,
    val isFlashEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val isLoading: Boolean = false
)
