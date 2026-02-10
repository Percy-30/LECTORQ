package com.scannerpro.lectorqr.domain.repository

import android.net.Uri
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import kotlinx.coroutines.flow.Flow

interface IScannerRepository {
    fun startScanning(): Flow<BarcodeResult>
    suspend fun processImageFromGallery(uri: Uri): BarcodeResult?
    suspend fun onBarcodeDetected(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?): Long
    fun toggleFlash(isEnabled: Boolean)
    fun flipCamera()
    suspend fun processBarcodeManually(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?): BarcodeResult?
}
