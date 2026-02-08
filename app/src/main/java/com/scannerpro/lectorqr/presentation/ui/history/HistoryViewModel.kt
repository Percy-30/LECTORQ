package com.scannerpro.lectorqr.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.ClearHistoryUseCase
import com.scannerpro.lectorqr.domain.usecase.GetHistoryUseCase
import com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryUiState(
    val groupedScans: Map<String, List<BarcodeResult>> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHistoryUseCase().collect { history ->
                val grouped = groupHistory(history)
                _uiState.update { it.copy(groupedScans = grouped, isLoading = false) }
            }
        }
    }

    private fun groupHistory(scans: List<BarcodeResult>): Map<String, List<BarcodeResult>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        return scans.groupBy { result ->
            val date = Instant.ofEpochMilli(result.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            when (date) {
                today -> "Hoy"
                yesterday -> "Ayer"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("d MMM. yyyy", java.util.Locale.getDefault())
                    date.format(formatter)
                }
            }
        }
    }

    fun toggleFavorite(scanId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(scanId, !isFavorite)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearHistoryUseCase()
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            deleteScanUseCase(id)
        }
    }

    fun updateName(id: Long, newName: String) {
        viewModelScope.launch {
            updateScanNameUseCase(id, newName)
        }
    }
}
