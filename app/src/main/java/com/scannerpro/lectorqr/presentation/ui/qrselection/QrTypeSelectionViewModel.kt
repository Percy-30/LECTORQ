package com.scannerpro.lectorqr.presentation.ui.qrselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrTypeSelectionUiState(
    val isPremium: Boolean = false
)

@HiltViewModel
class QrTypeSelectionViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrTypeSelectionUiState())
    val uiState: StateFlow<QrTypeSelectionUiState> = _uiState.asStateFlow()

    init {
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            settingsRepository.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
    }
}
