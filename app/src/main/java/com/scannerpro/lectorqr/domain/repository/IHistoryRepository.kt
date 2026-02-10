package com.scannerpro.lectorqr.domain.repository

import com.scannerpro.lectorqr.domain.model.BarcodeResult
import kotlinx.coroutines.flow.Flow

interface IHistoryRepository {
    fun getAllScans(): Flow<List<BarcodeResult>>
    fun getFavoriteScans(): Flow<List<BarcodeResult>>
    suspend fun getScanById(id: Long): BarcodeResult?
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun updateScanName(id: Long, name: String)
    suspend fun deleteScan(id: Long)
    suspend fun insertScan(scan: BarcodeResult): Long
    suspend fun clearHistory()
}
