package com.scannerpro.lectorqr.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ISettingsRepository {

    private val scope = kotlinx.coroutines.MainScope()

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val premiumPrefs: SharedPreferences = context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    override val themeMode: Flow<Int> = _themeMode.asStateFlow()

    private val _primaryColor = MutableStateFlow(prefs.getLong("primary_color", 0xFF2196F3))
    override val primaryColor: Flow<Long> = _primaryColor.asStateFlow()

    private val _isBeepEnabled = MutableStateFlow(prefs.getBoolean("beep_enabled", false))
    override val isBeepEnabled: Flow<Boolean> = _isBeepEnabled.asStateFlow()

    private val _isVibrateEnabled = MutableStateFlow(prefs.getBoolean("vibrate_enabled", false))
    override val isVibrateEnabled: Flow<Boolean> = _isVibrateEnabled.asStateFlow()

    private val _isCopyToClipboardEnabled = MutableStateFlow(prefs.getBoolean("copy_to_clipboard", false))
    override val isCopyToClipboardEnabled: Flow<Boolean> = _isCopyToClipboardEnabled.asStateFlow()

    private val _isUrlInfoEnabled = MutableStateFlow(prefs.getBoolean("url_info", true))
    override val isUrlInfoEnabled: Flow<Boolean> = _isUrlInfoEnabled.asStateFlow()

    private val _isBatchScanEnabled = MutableStateFlow(prefs.getBoolean("batch_scan", false))
    override val isBatchScanEnabled: Flow<Boolean> = _isBatchScanEnabled.asStateFlow()

    private val _isAutofocusEnabled = MutableStateFlow(prefs.getBoolean("autofocus", true))
    override val isAutofocusEnabled: Flow<Boolean> = _isAutofocusEnabled.asStateFlow()

    private val _isTapToFocusEnabled = MutableStateFlow(prefs.getBoolean("tap_to_focus", true))
    override val isTapToFocusEnabled: Flow<Boolean> = _isTapToFocusEnabled.asStateFlow()

    private val _isKeepDuplicatesEnabled = MutableStateFlow(prefs.getBoolean("keep_duplicates", true))
    override val isKeepDuplicatesEnabled: Flow<Boolean> = _isKeepDuplicatesEnabled.asStateFlow()

    private val _isAppBrowserEnabled = MutableStateFlow(prefs.getBoolean("app_browser", true))
    override val isAppBrowserEnabled: Flow<Boolean> = _isAppBrowserEnabled.asStateFlow()

    private val _isAddToHistoryEnabled = MutableStateFlow(prefs.getBoolean("add_to_history", true))
    override val isAddToHistoryEnabled: Flow<Boolean> = _isAddToHistoryEnabled.asStateFlow()

    private val _isOpenUrlAutomaticallyEnabled = MutableStateFlow(prefs.getBoolean("open_url_auto", true))
    override val isOpenUrlAutomaticallyEnabled: Flow<Boolean> = _isOpenUrlAutomaticallyEnabled.asStateFlow()

    private val _cameraSelection = MutableStateFlow(prefs.getInt("camera_selection", 0))
    override val cameraSelection: Flow<Int> = _cameraSelection.asStateFlow()

    private val _searchEngine = MutableStateFlow(prefs.getString("search_engine", "Google") ?: "Google")
    override val searchEngine: Flow<String> = _searchEngine.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(prefs.getString("selected_language", "system") ?: "system")
    override val selectedLanguage: Flow<String> = _selectedLanguage.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    override val isBiometricEnabled: Flow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _currentAppIcon = MutableStateFlow(prefs.getString("current_app_icon", "DEFAULT") ?: "DEFAULT")
    override val currentAppIcon: Flow<String> = _currentAppIcon.asStateFlow()

    private val _isRealPremium = MutableStateFlow(premiumPrefs.getBoolean("is_premium", false))
    
    private val _isManualPremium = MutableStateFlow(premiumPrefs.getBoolean("is_manual_premium", false))
    override val isManualPremium: StateFlow<Boolean> = _isManualPremium.asStateFlow()

    override val isPremium: StateFlow<Boolean> = combine(_isRealPremium, _isManualPremium) { real, manual ->
        real || manual
    }.stateIn(scope, SharingStarted.Eagerly, _isRealPremium.value || _isManualPremium.value)

    override suspend fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    override suspend fun setPrimaryColor(color: Long) {
        prefs.edit().putLong("primary_color", color).apply()
        _primaryColor.value = color
    }

    override suspend fun setBeepEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("beep_enabled", enabled).apply()
        _isBeepEnabled.value = enabled
    }

    override suspend fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_enabled", enabled).apply()
        _isVibrateEnabled.value = enabled
    }

    override suspend fun setCopyToClipboardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("copy_to_clipboard", enabled).apply()
        _isCopyToClipboardEnabled.value = enabled
    }

    override suspend fun setUrlInfoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("url_info", enabled).apply()
        _isUrlInfoEnabled.value = enabled
    }

    override suspend fun setBatchScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("batch_scan", enabled).apply()
        _isBatchScanEnabled.value = enabled
    }

    override suspend fun setAutofocusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("autofocus", enabled).apply()
        _isAutofocusEnabled.value = enabled
    }

    override suspend fun setTapToFocusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tap_to_focus", enabled).apply()
        _isTapToFocusEnabled.value = enabled
    }

    override suspend fun setKeepDuplicatesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("keep_duplicates", enabled).apply()
        _isKeepDuplicatesEnabled.value = enabled
    }

    override suspend fun setAppBrowserEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_browser", enabled).apply()
        _isAppBrowserEnabled.value = enabled
    }

    override suspend fun setAddToHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("add_to_history", enabled).apply()
        _isAddToHistoryEnabled.value = enabled
    }

    override suspend fun setOpenUrlAutomaticallyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("open_url_auto", enabled).apply()
        _isOpenUrlAutomaticallyEnabled.value = enabled
    }

    override suspend fun setCameraSelection(camera: Int) {
        prefs.edit().putInt("camera_selection", camera).apply()
        _cameraSelection.value = camera
    }

    override suspend fun setSearchEngine(engine: String) {
        prefs.edit().putString("search_engine", engine).apply()
        _searchEngine.value = engine
    }

    override suspend fun setPremium(enabled: Boolean) {
        android.util.Log.d("SettingsRepository", "setPremium: $enabled")
        premiumPrefs.edit().putBoolean("is_premium", enabled).commit()
        _isRealPremium.value = enabled
    }

    override suspend fun setManualPremium(enabled: Boolean) {
        premiumPrefs.edit().putBoolean("is_manual_premium", enabled).commit()
        _isManualPremium.value = enabled
    }

    override suspend fun setSelectedLanguage(languageCode: String) {
        prefs.edit().putString("selected_language", languageCode).apply()
        _selectedLanguage.value = languageCode
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    override suspend fun setCurrentAppIcon(iconName: String) {
        prefs.edit().putString("current_app_icon", iconName).apply()
        _currentAppIcon.value = iconName
    }
}
