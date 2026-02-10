package com.scannerpro.lectorqr.domain.repository

import android.graphics.Bitmap

interface IQRGeneratorRepository {
    suspend fun generateQrCode(text: String, width: Int, height: Int): Bitmap?
    suspend fun generateBarcode(text: String, format: Int, width: Int, height: Int): Bitmap?
}
