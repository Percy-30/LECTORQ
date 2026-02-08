package com.scannerpro.lectorqr.di

import android.content.Context
import androidx.room.Room
import com.scannerpro.lectorqr.data.local.ScanDatabase
import com.scannerpro.lectorqr.data.local.dao.ScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanDatabase {
        return Room.databaseBuilder(
            context,
            ScanDatabase::class.java,
            "lector_qr.db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideScanDao(database: ScanDatabase): ScanDao {
        return database.scanDao
    }
}
