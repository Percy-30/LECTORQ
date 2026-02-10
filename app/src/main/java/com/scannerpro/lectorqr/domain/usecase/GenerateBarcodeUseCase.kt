package com.scannerpro.lectorqr.domain.usecase

import android.graphics.Bitmap
import com.scannerpro.lectorqr.domain.repository.IQRGeneratorRepository
import javax.inject.Inject

class GenerateBarcodeUseCase @Inject constructor(
    private val repository: IQRGeneratorRepository
) {
    suspend operator fun invoke(text: String, format: Int, width: Int = 512, height: Int = 512): Bitmap? {
        if (text.isBlank()) return null
        return repository.generateBarcode(text, format, width, height)
    }
}
