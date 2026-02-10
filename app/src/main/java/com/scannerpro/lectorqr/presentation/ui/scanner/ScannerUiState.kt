package com.scannerpro.lectorqr.presentation.ui.scanner

import com.scannerpro.lectorqr.domain.model.BarcodeResult

data class ScannerUiState(
    val lastResult: BarcodeResult? = null,
    val isFlashEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val isLoading: Boolean = false,
    val isGalleryRequested: Boolean = false,
    val isAutofocusEnabled: Boolean = true,
    val isTapToFocusEnabled: Boolean = true,
    val cameraSelection: Int = 0,
    val isBatchScanEnabled: Boolean = false,
    val isBatchModeActive: Boolean = false,
    val isKeepDuplicatesEnabled: Boolean = true,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val isPremium: Boolean = false
)
