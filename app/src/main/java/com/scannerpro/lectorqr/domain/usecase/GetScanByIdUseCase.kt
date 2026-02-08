package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import javax.inject.Inject

class GetScanByIdUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke(id: Long): BarcodeResult? = repository.getScanById(id)
}
