package com.scannerpro.lectorqr.presentation.ui.favorites

import com.scannerpro.lectorqr.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.GetFavoritesUseCase
import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import com.scannerpro.lectorqr.util.FileUtils
import javax.inject.Inject

data class FavoritesUiState(
    val scans: List<BarcodeResult> = emptyList(),
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val selectedFilter: Int? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var favoritesJob: kotlinx.coroutines.Job? = null

    init {
        loadFavorites()
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

    private fun loadFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getFavoritesUseCase().collect { favorites ->
                val filtered = if (_uiState.value.selectedFilter != null) {
                    favorites.filter { it.type == _uiState.value.selectedFilter }
                } else {
                    favorites
                }
                _uiState.update { it.copy(scans = filtered, isLoading = false) }
            }
        }
    }

    fun setFilter(type: Int?) {
        _uiState.update { it.copy(selectedFilter = type) }
        loadFavorites()
    }

    fun toggleFavorite(scan: BarcodeResult) {
        viewModelScope.launch {
            toggleFavoriteUseCase(scan.id, !scan.isFavorite)
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

    fun exportIndividualAsTxt(scan: BarcodeResult, isShare: Boolean = false) {
        val defaultName = context.getString(com.scannerpro.lectorqr.util.BarcodeTypeUtils.getTypeNameRes(scan.type))
        val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
        val dateStr = java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))
        val formattedValue = com.scannerpro.lectorqr.util.BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
        val content = "${context.getString(com.scannerpro.lectorqr.R.string.export_name_label)} $displayName\n" +
                "${context.getString(com.scannerpro.lectorqr.R.string.export_content_label)} $formattedValue\n" +
                "${context.getString(com.scannerpro.lectorqr.R.string.export_date_label)} $dateStr"
        val filename = "$displayName.txt"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportIndividualAsCsv(scan: BarcodeResult, isShare: Boolean = false) {
        val defaultName = context.getString(com.scannerpro.lectorqr.util.BarcodeTypeUtils.getTypeNameRes(scan.type))
        val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
        val header = "${context.getString(com.scannerpro.lectorqr.R.string.csv_header_name)},${context.getString(com.scannerpro.lectorqr.R.string.csv_header_content)},${context.getString(com.scannerpro.lectorqr.R.string.csv_header_date)}\n"
        val formattedValue = com.scannerpro.lectorqr.util.BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue).replace("\n", " ").replace("\"", "\"\"")
        val row = "\"$displayName\",\"$formattedValue\",\"${scan.timestamp}\""
        val filename = "$displayName.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", header + row)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", header + row)
        }
    }

    fun exportGroupAsTxt(scans: List<BarcodeResult>, isShare: Boolean = false) {
        val content = scans.joinToString("\n\n---\n\n") { scan ->
            val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
            val dateStr = java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))
            "${context.getString(R.string.export_name_label)} $displayName\n" +
                    "${context.getString(R.string.export_content_label)} $formattedValue\n" +
                    "${context.getString(R.string.export_date_label)} $dateStr"
        }
        val filename = "${context.getString(R.string.drawer_favorites)}.txt"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportGroupAsCsv(scans: List<BarcodeResult>, isShare: Boolean = false) {
        val header = "${context.getString(R.string.csv_header_name)},${context.getString(R.string.csv_header_content)},${context.getString(R.string.csv_header_date)}\n"
        val rows = scans.joinToString("\n") { scan ->
            val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue).replace("\n", " ").replace("\"", "\"\"")
            "\"$displayName\",\"$formattedValue\",\"${scan.timestamp}\""
        }
        val filename = "${context.getString(R.string.drawer_favorites)}.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", header + rows)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", header + rows)
        }
    }

    fun shareGroup(scans: List<BarcodeResult>) {
        val content = scans.joinToString("\n\n---\n\n") { scan ->
            val defaultName = context.getString(BarcodeTypeUtils.getTypeNameRes(scan.type))
            val displayName = if (scan.customName != null && scan.customName != "Texto") scan.customName else defaultName
            val formattedValue = BarcodeTypeUtils.getFormattedValue(context, scan.type, scan.rawValue)
            "${context.getString(R.string.export_name_label)} $displayName\n" +
                    "${context.getString(R.string.export_content_label)} $formattedValue"
        }
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, content)
            type = "text/plain"
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Compartir grupo de favoritos")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
