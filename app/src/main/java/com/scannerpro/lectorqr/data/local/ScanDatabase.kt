package com.scannerpro.lectorqr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scannerpro.lectorqr.data.local.dao.ScanDao
import com.scannerpro.lectorqr.data.local.entity.ScanEntity

@Database(entities = [ScanEntity::class], version = 2, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {
    abstract val scanDao: ScanDao
}
