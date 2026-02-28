package com.scannerpro.lectorqr.domain.usecase

import android.graphics.Bitmap
import com.scannerpro.lectorqr.domain.repository.IQRGeneratorRepository
import javax.inject.Inject

class GenerateQrUseCase @Inject constructor(
    private val repository: IQRGeneratorRepository
) {
    suspend operator fun invoke(
        text: String, 
        size: Int = 512,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE,
        logo: Bitmap? = null
    ): Bitmap? {
        if (text.isBlank()) return null
        return repository.generateQrCode(text, size, size, foregroundColor, backgroundColor, logo)
    }
}
