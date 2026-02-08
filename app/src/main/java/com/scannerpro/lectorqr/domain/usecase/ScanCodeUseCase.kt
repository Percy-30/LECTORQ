package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IScannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanCodeUseCase @Inject constructor(
    private val repository: IScannerRepository
) {
    operator fun invoke(): Flow<BarcodeResult> {
        return repository.startScanning()
    }
}
