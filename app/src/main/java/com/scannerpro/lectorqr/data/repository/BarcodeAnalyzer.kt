package com.scannerpro.lectorqr.data.repository

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onBarcodesDetected: (List<com.google.mlkit.vision.barcode.common.Barcode>, android.graphics.Bitmap?, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        
                        // Capture bitmap safely (before proxy is closed)
                        // Rotate and crop bitmap safely based on the first barcode (or overall bounding box)
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

                        // For multi-barcode, we just pass the full rotated bitmap, or crop to the first one for backwards compatibility
                        val croppedBitmap = rotatedBitmap?.let { bitmap ->
                            barcodes.firstOrNull()?.boundingBox?.let { rect ->
                                try {
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
                        val sourceWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                        val sourceHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
                        
                        onBarcodesDetected(barcodes, croppedBitmap, sourceWidth, sourceHeight)
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
