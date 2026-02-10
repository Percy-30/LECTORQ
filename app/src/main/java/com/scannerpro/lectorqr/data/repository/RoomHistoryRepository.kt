package com.scannerpro.lectorqr.data.repository

import com.scannerpro.lectorqr.data.local.dao.ScanDao
import com.scannerpro.lectorqr.data.local.entity.toDomain
import com.scannerpro.lectorqr.data.local.entity.toEntity
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomHistoryRepository @Inject constructor(
    private val scanDao: ScanDao
) : IHistoryRepository {
    override fun getAllScans(): Flow<List<BarcodeResult>> = 
        scanDao.getAllScans().map { list -> list.map { it.toDomain() } }

    override fun getFavoriteScans(): Flow<List<BarcodeResult>> = 
        scanDao.getFavoriteScans().map { list -> list.map { it.toDomain() } }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = 
        scanDao.updateFavorite(id, isFavorite)

    override suspend fun updateScanName(id: Long, name: String) =
        scanDao.updateName(id, name)

    override suspend fun getScanById(id: Long): BarcodeResult? =
        scanDao.getScanById(id)?.toDomain()

    override suspend fun deleteScan(id: Long) = scanDao.deleteById(id)

    override suspend fun insertScan(scan: BarcodeResult): Long = 
        scanDao.insertScan(scan.toEntity())

    override suspend fun clearHistory() = scanDao.clearHistory()
}
