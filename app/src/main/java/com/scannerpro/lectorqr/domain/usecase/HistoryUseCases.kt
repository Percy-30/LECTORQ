package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    operator fun invoke(): Flow<List<BarcodeResult>> = repository.getAllScans()
}

class GetFavoritesUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    operator fun invoke(): Flow<List<BarcodeResult>> = repository.getFavoriteScans()
}

class ClearHistoryUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke() = repository.clearHistory()
}
