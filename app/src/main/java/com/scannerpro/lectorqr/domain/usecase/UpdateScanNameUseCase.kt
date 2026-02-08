package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import javax.inject.Inject

class UpdateScanNameUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke(id: Long, name: String) {
        if (name.isNotBlank()) {
            repository.updateScanName(id, name)
        }
    }
}
