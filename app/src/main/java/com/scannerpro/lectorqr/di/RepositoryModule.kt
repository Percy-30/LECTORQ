package com.scannerpro.lectorqr.di

import com.scannerpro.lectorqr.data.repository.MLKitScannerRepository
import com.scannerpro.lectorqr.data.repository.RoomHistoryRepository
import com.scannerpro.lectorqr.data.repository.ZxingQRGeneratorRepository
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import com.scannerpro.lectorqr.domain.repository.IQRGeneratorRepository
import com.scannerpro.lectorqr.domain.repository.IScannerRepository
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import com.scannerpro.lectorqr.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScannerRepository(
        mlKitScannerRepository: MLKitScannerRepository
    ): IScannerRepository

    @Binds
    @Singleton
    abstract fun bindQRGeneratorRepository(
        zxingQRGeneratorRepository: ZxingQRGeneratorRepository
    ): IQRGeneratorRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        roomHistoryRepository: RoomHistoryRepository
    ): IHistoryRepository
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): ISettingsRepository
}
