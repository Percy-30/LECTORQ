package com.scannerpro.lectorqr.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {
    val themeMode: Flow<Int> // 0: System, 1: Light, 2: Dark
    val primaryColor: Flow<Long>
    val isBeepEnabled: Flow<Boolean>
    val isVibrateEnabled: Flow<Boolean>
    val isCopyToClipboardEnabled: Flow<Boolean>
    val isUrlInfoEnabled: Flow<Boolean>
    val isBatchScanEnabled: Flow<Boolean>
    val isAutofocusEnabled: Flow<Boolean>
    val isTapToFocusEnabled: Flow<Boolean>
    val isKeepDuplicatesEnabled: Flow<Boolean>
    val isAppBrowserEnabled: Flow<Boolean>
    val isAddToHistoryEnabled: Flow<Boolean>
    val isOpenUrlAutomaticallyEnabled: Flow<Boolean>
    val cameraSelection: Flow<Int>
    val searchEngine: Flow<String>
    val isPremium: StateFlow<Boolean>
    val isManualPremium: StateFlow<Boolean>

    suspend fun setThemeMode(mode: Int)
    suspend fun setPrimaryColor(color: Long)
    suspend fun setBeepEnabled(enabled: Boolean)
    suspend fun setVibrateEnabled(enabled: Boolean)
    suspend fun setCopyToClipboardEnabled(enabled: Boolean)
    suspend fun setUrlInfoEnabled(enabled: Boolean)
    suspend fun setBatchScanEnabled(enabled: Boolean)
    suspend fun setAutofocusEnabled(enabled: Boolean)
    suspend fun setTapToFocusEnabled(enabled: Boolean)
    suspend fun setKeepDuplicatesEnabled(enabled: Boolean)
    suspend fun setAppBrowserEnabled(enabled: Boolean)
    suspend fun setAddToHistoryEnabled(enabled: Boolean)
    suspend fun setOpenUrlAutomaticallyEnabled(enabled: Boolean)
    suspend fun setCameraSelection(camera: Int)
    suspend fun setSearchEngine(engine: String)
    suspend fun setPremium(enabled: Boolean)
    suspend fun setManualPremium(enabled: Boolean)
}
