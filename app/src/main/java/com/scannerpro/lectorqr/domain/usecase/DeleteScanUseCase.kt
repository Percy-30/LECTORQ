package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import javax.inject.Inject

class DeleteScanUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteScan(id)
}
