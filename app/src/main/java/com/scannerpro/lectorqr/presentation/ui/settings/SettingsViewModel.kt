package com.scannerpro.lectorqr.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsRepository: ISettingsRepository,
    private val iconManager: com.scannerpro.lectorqr.util.IconManager
) : ViewModel() {

    val isPremium = settingsRepository.isPremium
    val selectedLanguage = settingsRepository.selectedLanguage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val themeMode = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val primaryColor = settingsRepository.primaryColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF2196F3)
    val isBeepEnabled = settingsRepository.isBeepEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isVibrateEnabled = settingsRepository.isVibrateEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isCopyToClipboardEnabled = settingsRepository.isCopyToClipboardEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isUrlInfoEnabled = settingsRepository.isUrlInfoEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isBatchScanEnabled = settingsRepository.isBatchScanEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isAutofocusEnabled = settingsRepository.isAutofocusEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isTapToFocusEnabled = settingsRepository.isTapToFocusEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isKeepDuplicatesEnabled = settingsRepository.isKeepDuplicatesEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isAppBrowserEnabled = settingsRepository.isAppBrowserEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isAddToHistoryEnabled = settingsRepository.isAddToHistoryEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isOpenUrlAutomaticallyEnabled = settingsRepository.isOpenUrlAutomaticallyEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val cameraSelection = settingsRepository.cameraSelection.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val searchEngine = settingsRepository.searchEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Google")
    val isBiometricEnabled = settingsRepository.isBiometricEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val currentAppIcon = settingsRepository.currentAppIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")
    val isManualPremium = settingsRepository.isManualPremium

    fun setThemeMode(mode: Int) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setPrimaryColor(color: Long) = viewModelScope.launch { settingsRepository.setPrimaryColor(color) }
    fun setBeepEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBeepEnabled(enabled) }
    fun setVibrateEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setVibrateEnabled(enabled) }
    fun setCopyToClipboardEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setCopyToClipboardEnabled(enabled) }
    fun setUrlInfoEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setUrlInfoEnabled(enabled) }
    fun setBatchScanEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBatchScanEnabled(enabled) }
    fun setAutofocusEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutofocusEnabled(enabled) }
    fun setTapToFocusEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setTapToFocusEnabled(enabled) }
    fun setKeepDuplicatesEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setKeepDuplicatesEnabled(enabled) }
    fun setAppBrowserEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAppBrowserEnabled(enabled) }
    fun setAddToHistoryEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAddToHistoryEnabled(enabled) }
    fun setOpenUrlAutomaticallyEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setOpenUrlAutomaticallyEnabled(enabled) }
    fun setCameraSelection(camera: Int) = viewModelScope.launch { settingsRepository.setCameraSelection(camera) }
    fun setSearchEngine(engine: String) = viewModelScope.launch { settingsRepository.setSearchEngine(engine) }
    fun setManualPremium(enabled: Boolean) = viewModelScope.launch { settingsRepository.setManualPremium(enabled) }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }

    fun setCurrentAppIcon(icon: com.scannerpro.lectorqr.util.AppIcon) = viewModelScope.launch {
        settingsRepository.setCurrentAppIcon(icon.name)
        iconManager.changeIcon(icon)
    }

    fun setSelectedLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedLanguage(languageCode)
            val appLocale: androidx.core.os.LocaleListCompat = if (languageCode == "system") {
                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            } else {
                androidx.core.os.LocaleListCompat.forLanguageTags(languageCode)
            }
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}
