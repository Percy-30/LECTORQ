package com.scannerpro.lectorqr.presentation.ui.history

import com.scannerpro.lectorqr.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.ClearHistoryUseCase
import com.scannerpro.lectorqr.domain.usecase.GetHistoryUseCase
import com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import com.scannerpro.lectorqr.util.FileUtils

import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HistoryUiState(
    val groupedScans: Map<String, List<BarcodeResult>> = emptyMap(),
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val selectedFilter: Int? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var historyJob: kotlinx.coroutines.Job? = null

    init {
        loadHistory()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isBiometricEnabled.collect { enabled ->
                _uiState.update { it.copy(isBiometricEnabled = enabled) }
            }
        }
    }

    private fun loadHistory() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getHistoryUseCase().collect { history ->
                val filtered = applyFilter(history, _uiState.value.selectedFilter)
                val grouped = groupHistory(filtered)
                _uiState.update { it.copy(groupedScans = grouped, isLoading = false) }
            }
        }
    }

    private fun applyFilter(scans: List<BarcodeResult>, type: Int?): List<BarcodeResult> {
        if (type == null) return scans
        return scans.filter { it.type == type }
    }

    fun setFilter(type: Int?) {
        _uiState.update { it.copy(selectedFilter = type) }
        loadHistory()
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

    fun deleteScans(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { deleteScanUseCase(it) }
        }
    }

    fun exportGroupAsTxt(date: String, scans: List<BarcodeResult>, isShare: Boolean = false) {
        val content = scans.joinToString("\n\n---\n\n") { scan ->
            val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
            val dateStr = java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))
            "${context.getString(R.string.export_name_label)} $displayName\n" +
                    "${context.getString(R.string.export_content_label)} $formattedValue\n" +
                    "${context.getString(R.string.export_date_label)} $dateStr"
        }
        val filename = "Historial_$date.txt"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportGroupAsCsv(date: String, scans: List<BarcodeResult>, isShare: Boolean = false) {
        val header = "${context.getString(R.string.csv_header_name)},${context.getString(R.string.csv_header_content)},${context.getString(R.string.csv_header_date)}\n"
        val rows = scans.joinToString("\n") { scan ->
            val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue).replace("\n", " ").replace("\"", "\"\"")
            "\"$displayName\",\"$formattedValue\",\"${scan.timestamp}\""
        }
        val filename = "Historial_$date.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", header + rows)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", header + rows)
        }
    }

    fun exportIndividualAsTxt(scan: BarcodeResult, isShare: Boolean = false) {
        val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
        val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
        val dateStr = java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
        val content = "${context.getString(R.string.export_name_label)} $displayName\n" +
                "${context.getString(R.string.export_content_label)} $formattedValue\n" +
                "${context.getString(R.string.export_date_label)} $dateStr"
        val filename = "$displayName.txt"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportIndividualAsCsv(scan: BarcodeResult, isShare: Boolean = false) {
        val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
        val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
        val header = "${context.getString(R.string.csv_header_name)},${context.getString(R.string.csv_header_content)},${context.getString(R.string.csv_header_date)}\n"
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue).replace("\n", " ").replace("\"", "\"\"")
        val row = "\"$displayName\",\"$formattedValue\",\"${scan.timestamp}\""
        val filename = "$displayName.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", header + row)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", header + row)
        }
    }

    fun shareGroup(scans: List<BarcodeResult>) {
        val content = scans.joinToString("\n\n---\n\n") { scan ->
            val defaultName = context.getString(com.scannerpro.lectorqr.util.BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = com.scannerpro.lectorqr.util.BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
            "${context.getString(R.string.export_name_label)} $displayName\n" +
                    "${context.getString(R.string.export_content_label)} $formattedValue"
        }
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, content)
            type = "text/plain"
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Compartir grupo")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

}
