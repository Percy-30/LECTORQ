package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.usecase.GetScanByIdUseCase
import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanResultUiState(
    val result: BarcodeResult? = null,
    val isFavorite: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val renameInput: String = "",
    val customName: String = "Texto",
    val isLoading: Boolean = false,
    val searchEngine: String = "Google",
    val isAppBrowserEnabled: Boolean = true
)

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getScanByIdUseCase: GetScanByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val deleteScanUseCase: com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanResultUiState())
    val uiState: StateFlow<ScanResultUiState> = _uiState.asStateFlow()
    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.searchEngine.collect { engine ->
                _uiState.update { it.copy(searchEngine = engine) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isAppBrowserEnabled.collect { enabled ->
                _uiState.update { it.copy(isAppBrowserEnabled = enabled) }
            }
        }
    }

    fun init(scanId: Long) {
        // If we already have the result (e.g. just scanned), we might already have it in state
        if (_uiState.value.result?.id == scanId) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getScanByIdUseCase(scanId)
            if (result != null) {
                _uiState.update { 
                    it.copy(
                        result = result, 
                        isFavorite = result.isFavorite, 
                        customName = result.customName ?: "Texto",
                        renameInput = result.customName ?: "Texto",
                        isLoading = false
                    ) 
                }
            } else {
                // Fallback or error state
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite() {
        val currentResult = _uiState.value.result ?: return
        val newFavoriteStatus = !currentResult.isFavorite
        viewModelScope.launch {
            toggleFavoriteUseCase(currentResult.id, newFavoriteStatus)
            _uiState.update { 
                it.copy(
                    isFavorite = newFavoriteStatus,
                    result = currentResult.copy(isFavorite = newFavoriteStatus)
                ) 
            }
        }
    }

    fun openRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = true) }
    }

    fun closeRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun updateRenameInput(name: String) {
        _uiState.update { it.copy(renameInput = name) }
    }

    fun saveName() {
        val currentResult = _uiState.value.result ?: return
        val newName = _uiState.value.renameInput
        viewModelScope.launch {
            updateScanNameUseCase(currentResult.id, newName)
            _uiState.update { 
                it.copy(
                    isRenameDialogOpen = false,
                    customName = newName,
                    result = currentResult.copy(customName = newName)
                ) 
            }
        }
    }

    fun deleteScan() {
        val currentResult = _uiState.value.result ?: return
        viewModelScope.launch {
            deleteScanUseCase(currentResult.id)
            _uiState.update { ScanResultUiState() }
        }
    }

    fun exportAsTxt() {
        val result = _uiState.value.result ?: return
        val content = """
            Nombre: ${_uiState.value.customName}
            Contenido: ${result.rawValue}
            Fecha: ${java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))}
        """.trimIndent()
        saveFileToDownloads("${_uiState.value.customName}.txt", "text/plain", content)
    }

    fun exportAsCsv() {
        val result = _uiState.value.result ?: return
        val content = "Nombre,Contenido,Fecha\n" +
                "\"${_uiState.value.customName}\",\"${result.rawValue}\",\"${result.timestamp}\""
        saveFileToDownloads("${_uiState.value.customName}.csv", "text/csv", content)
    }

    private fun saveFileToDownloads(filename: String, mimeType: String, content: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            stream.write(content.toByteArray())
                        }
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "$filename guardado en Descargas", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, filename)
                    java.io.FileOutputStream(file).use { stream ->
                        stream.write(content.toByteArray())
                    }
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "$filename guardado en Descargas", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScanResultVM", "Error exporting file", e)
            }
        }
    }

    fun getSearchUrl(query: String): String {
        return when (_uiState.value.searchEngine) {
            "Bing" -> "https://www.bing.com/search?q=$query"
            "Yahoo" -> "https://search.yahoo.com/search?p=$query"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$query"
            "Yandex" -> "https://yandex.com/search/?text=$query"
            else -> "https://www.google.com/search?q=$query"
        }
    }
}
