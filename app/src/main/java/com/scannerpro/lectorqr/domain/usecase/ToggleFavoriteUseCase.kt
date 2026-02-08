package com.scannerpro.lectorqr.domain.usecase

import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: IHistoryRepository
) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean) {
        repository.toggleFavorite(id, isFavorite)
    }
}
