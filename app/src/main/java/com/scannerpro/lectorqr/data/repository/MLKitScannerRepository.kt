package com.scannerpro.lectorqr.data.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.scannerpro.lectorqr.data.local.dao.ScanDao
import com.scannerpro.lectorqr.data.local.entity.toEntity
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IScannerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MLKitScannerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao
) : IScannerRepository {

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(scannerOptions)
    private val scanResults = MutableSharedFlow<BarcodeResult>(replay = 0)

    override fun startScanning(): Flow<BarcodeResult> {
        return scanResults
    }

    override suspend fun onBarcodeDetected(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?): Long {
        val imagePath = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            bitmap?.let { saveBitmap(it) }
        }
        val result = BarcodeResult(
            displayValue = barcode.displayValue,
            rawValue = barcode.rawValue,
            format = barcode.format,
            type = barcode.valueType,
            imagePath = imagePath,
            customName = "Texto"
        )
        // Record in history and get generated ID
        val insertedId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            scanDao.insertScan(result.toEntity())
        }
        
        // Emit result with correct ID
        scanResults.emit(result.copy(id = insertedId))
        return insertedId
    }

    private fun saveBitmap(bitmap: android.graphics.Bitmap): String? {
        return try {
            val fileName = "scan_${System.currentTimeMillis()}.jpg"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            context.getFileStreamPath(fileName).absolutePath
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun processImageFromGallery(uri: Uri): BarcodeResult? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val barcodes = scanner.process(image).await()
            if (barcodes.isNotEmpty()) {
                val barcode = barcodes[0]
                val result = BarcodeResult(
                    displayValue = barcode.displayValue,
                    rawValue = barcode.rawValue,
                    format = barcode.format,
                    type = barcode.valueType
                )
                // Record in history and get ID
                val insertedId = scanDao.insertScan(result.toEntity())
                result.copy(id = insertedId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun toggleFlash(isEnabled: Boolean) {}

    override fun flipCamera() {}
}
