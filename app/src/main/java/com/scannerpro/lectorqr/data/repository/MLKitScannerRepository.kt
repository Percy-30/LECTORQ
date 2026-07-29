package com.scannerpro.lectorqr.data.repository

import android.util.Log

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.scannerpro.lectorqr.data.local.dao.ScanDao
import com.scannerpro.lectorqr.data.local.entity.toEntity
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
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

    private val scannerOptions = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(scannerOptions)
    private val scanResults = MutableSharedFlow<BarcodeResult>(replay = 1)

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
            customName = context.getString(BarcodeTypeUtils.getTypeNameRes(barcode.valueType))
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

    override suspend fun processImageFromGallery(uri: Uri): Pair<List<com.google.mlkit.vision.barcode.common.Barcode>, android.graphics.Bitmap?>? {
        Log.e("ScannerRepo", "processImageFromGallery: uri=$uri")
        return try {
            val fullBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                // Calculate inSampleSize (max dimension around 3840 to avoid OOM and keep ML Kit fast, allows dense QR detection)
                var inSampleSize = 1
                val maxDim = 3840
                if (options.outHeight > maxDim || options.outWidth > maxDim) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while (halfHeight / inSampleSize >= maxDim || halfWidth / inSampleSize >= maxDim) {
                        inSampleSize *= 2
                    }
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                
                inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                bitmap
            } ?: run {
                Log.e("ScannerRepo", "failed to decode bitmap from uri")
                return null
            }
            
            Log.e("ScannerRepo", "image loaded safely, size: ${fullBitmap.width}x${fullBitmap.height}")
            val image = InputImage.fromBitmap(fullBitmap, 0)
            val barcodes = scanner.process(image).await()
            Log.e("ScannerRepo", "ML Kit processing done, detected ${barcodes.size} barcodes")
            
            if (barcodes.isNotEmpty()) {
                Pair(barcodes, fullBitmap)
            } else {
                Log.e("ScannerRepo", "No barcodes detected in image")
                null
            }
        } catch (e: Exception) {
            Log.e("ScannerRepo", "Error processing image from gallery", e)
            null
        }
    }

    override fun toggleFlash(isEnabled: Boolean) {}

    override fun flipCamera() {}

    override suspend fun processBarcodeManually(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?): BarcodeResult? {
        val imagePath = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            bitmap?.let { saveBitmap(it) }
        }
        return BarcodeResult(
            displayValue = barcode.displayValue,
            rawValue = barcode.rawValue,
            format = barcode.format,
            type = barcode.valueType,
            imagePath = imagePath,
            customName = context.getString(BarcodeTypeUtils.getTypeNameRes(barcode.valueType))
        )
    }
}
