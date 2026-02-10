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

    override suspend fun generateQrCode(text: String, width: Int, height: Int): Bitmap? {
        return generateBarcode(text, 256, width, height) // 256 = QR_CODE
    }

    override suspend fun generateBarcode(text: String, format: Int, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val barcodeFormat = when (format) {
                1 -> BarcodeFormat.CODE_128
                2 -> BarcodeFormat.CODE_39
                4 -> BarcodeFormat.CODE_93
                8 -> BarcodeFormat.CODABAR
                16 -> BarcodeFormat.DATA_MATRIX
                32 -> BarcodeFormat.EAN_13
                64 -> BarcodeFormat.EAN_8
                128 -> BarcodeFormat.ITF
                256 -> BarcodeFormat.QR_CODE
                512 -> BarcodeFormat.UPC_A
                1024 -> BarcodeFormat.UPC_E
                2048 -> BarcodeFormat.PDF_417
                4096 -> BarcodeFormat.AZTEC
                else -> BarcodeFormat.QR_CODE
            }

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                barcodeFormat,
                width,
                height
            )
            
            // Adjust dimensions for linear barcodes (1D) if necessary, 
            // but ZXing handles this. 1D barcodes need more width than height.
            val actualWidth = bitMatrix.width
            val actualHeight = bitMatrix.height
            
            val bitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565)
            for (x in 0 until actualWidth) {
                for (y in 0 until actualHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ZxingRepo", "Error generating barcode format $format", e)
            null
        }
    }
}
