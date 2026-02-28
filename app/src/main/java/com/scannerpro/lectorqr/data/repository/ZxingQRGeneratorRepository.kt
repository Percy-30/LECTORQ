package com.scannerpro.lectorqr.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.scannerpro.lectorqr.domain.repository.IQRGeneratorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ZxingQRGeneratorRepository @Inject constructor() : IQRGeneratorRepository {

    override suspend fun generateQrCode(text: String, width: Int, height: Int, foregroundColor: Int, backgroundColor: Int, logo: Bitmap?): Bitmap? {
        return generateBarcode(text, 256, width, height, foregroundColor, backgroundColor, logo) // 256 = QR_CODE
    }

    override suspend fun generateBarcode(text: String, format: Int, width: Int, height: Int, foregroundColor: Int, backgroundColor: Int, logo: Bitmap?): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val barcodeFormat = when (format) {
                1 -> com.google.zxing.BarcodeFormat.CODE_128
                2 -> com.google.zxing.BarcodeFormat.CODE_39
                4 -> com.google.zxing.BarcodeFormat.CODE_93
                8 -> com.google.zxing.BarcodeFormat.CODABAR
                16 -> com.google.zxing.BarcodeFormat.DATA_MATRIX
                32 -> com.google.zxing.BarcodeFormat.EAN_13
                64 -> com.google.zxing.BarcodeFormat.EAN_8
                128 -> com.google.zxing.BarcodeFormat.ITF
                256 -> com.google.zxing.BarcodeFormat.QR_CODE
                512 -> com.google.zxing.BarcodeFormat.UPC_A
                1024 -> com.google.zxing.BarcodeFormat.UPC_E
                2048 -> com.google.zxing.BarcodeFormat.PDF_417
                4096 -> com.google.zxing.BarcodeFormat.AZTEC
                else -> com.google.zxing.BarcodeFormat.QR_CODE
            }

            val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
            if (barcodeFormat == com.google.zxing.BarcodeFormat.QR_CODE) {
                hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                hints[com.google.zxing.EncodeHintType.MARGIN] = 1
            }

            val bitMatrix: com.google.zxing.common.BitMatrix = com.google.zxing.MultiFormatWriter().encode(
                text,
                barcodeFormat,
                width,
                height,
                hints
            )
            
            val actualWidth = bitMatrix.width
            val actualHeight = bitMatrix.height
            
            val bitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
            for (x in 0 until actualWidth) {
                for (y in 0 until actualHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) foregroundColor else backgroundColor)
                }
            }

            // Draw logo if provided (only for QR_CODE usually)
            if (logo != null && barcodeFormat == com.google.zxing.BarcodeFormat.QR_CODE) {
                val canvas = android.graphics.Canvas(bitmap)
                
                // Calculate logo size (max 20% of QR size)
                val logoSize = (actualWidth * 0.2f).toInt()
                val logoX = (actualWidth - logoSize) / 2
                val logoY = (actualHeight - logoSize) / 2
                
                // Removed background square to allow transparent logos as requested
                // Draw logo
                val logoRect = android.graphics.Rect(0, 0, logo.width, logo.height)
                val destRect = android.graphics.Rect(logoX, logoY, logoX + logoSize, logoY + logoSize)
                canvas.drawBitmap(logo, logoRect, destRect, null)
            }

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ZxingRepo", "Error generating barcode format $format", e)
            null
        }
    }
}
