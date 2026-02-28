package com.scannerpro.lectorqr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scannerpro.lectorqr.data.local.dao.ScanDao
import com.scannerpro.lectorqr.data.local.entity.ScanEntity

@Database(entities = [ScanEntity::class], version = 3, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {
    abstract val scanDao: ScanDao

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scans ADD COLUMN foregroundColor INTEGER")
                db.execSQL("ALTER TABLE scans ADD COLUMN backgroundColor INTEGER")
            }
        }
    }
}
