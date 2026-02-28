package com.scannerpro.lectorqr.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onBarcodeDetected: (com.google.mlkit.vision.barcode.common.Barcode, android.graphics.Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {

    private var isDetected = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isDetected) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && !isDetected) {
                        isDetected = true
                        val barcode = barcodes[0]
                        
                        // Capture bitmap safely (before proxy is closed)
                        // Rotate and crop bitmap safely
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val fullBitmap = try { imageProxy.toBitmap() } catch (e: Exception) { null }
                        val rotatedBitmap = fullBitmap?.let { src ->
                            if (rotation != 0) {
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                android.graphics.Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
                            } else {
                                src
                            }
                        }

                        val croppedBitmap = rotatedBitmap?.let { bitmap ->
                            barcode.boundingBox?.let { rect ->
                                try {
                                    // Add padding for a "Quiet Zone" (approx 15%)
                                    val paddingW = (rect.width() * 0.15f).toInt()
                                    val paddingH = (rect.height() * 0.15f).toInt()
                                    
                                    val left = (rect.left - paddingW).coerceIn(0, bitmap.width)
                                    val top = (rect.top - paddingH).coerceIn(0, bitmap.height)
                                    val right = (rect.right + paddingW).coerceIn(0, bitmap.width)
                                    val bottom = (rect.bottom + paddingH).coerceIn(0, bitmap.height)
                                    
                                    val width = (right - left)
                                    val height = (bottom - top)
                                    
                                    if (width > 0 && height > 0) {
                                        android.graphics.Bitmap.createBitmap(bitmap, left, top, width, height)
                                    } else {
                                        bitmap
                                    }
                                } catch (e: Exception) {
                                    bitmap
                                }
                            } ?: bitmap
                        }
                        onBarcodeDetected(barcode, croppedBitmap)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
