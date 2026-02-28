package com.scannerpro.lectorqr.presentation.ui.scanner

import com.scannerpro.lectorqr.R

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.domain.repository.IScannerRepository
import com.scannerpro.lectorqr.domain.usecase.ScanCodeUseCase
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import com.scannerpro.lectorqr.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

import com.scannerpro.lectorqr.domain.usecase.ToggleFavoriteUseCase
import com.scannerpro.lectorqr.domain.usecase.UpdateScanNameUseCase

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val scanCodeUseCase: ScanCodeUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val repository: IScannerRepository,
    private val deleteScanUseCase: com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _scanResultUiState = MutableStateFlow(ScanResultUiState())
    val scanResultUiState: StateFlow<ScanResultUiState> = _scanResultUiState.asStateFlow()

    private val _interstitialTrigger = MutableSharedFlow<Unit>()
    val interstitialTrigger = _interstitialTrigger.asSharedFlow()
    private var scanCount = 0

    init {
        observeScanResults()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.isAutofocusEnabled.collect { enabled ->
                _uiState.update { it.copy(isAutofocusEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isTapToFocusEnabled.collect { enabled ->
                _uiState.update { it.copy(isTapToFocusEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.cameraSelection.collect { selection ->
                _uiState.update { it.copy(cameraSelection = selection) }
            }
        }
        viewModelScope.launch {
            settingsRepository.searchEngine.collect { engine ->
                _scanResultUiState.update { it.copy(searchEngine = engine) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isAppBrowserEnabled.collect { enabled ->
                _scanResultUiState.update { it.copy(isAppBrowserEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isBatchScanEnabled.collect { enabled ->
                _uiState.update { it.copy(isBatchScanEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isKeepDuplicatesEnabled.collect { enabled ->
                _uiState.update { it.copy(isKeepDuplicatesEnabled = enabled) }
            }
        }
    }

    private fun observeScanResults() {
        viewModelScope.launch {
            scanCodeUseCase().collect { result ->
                if (!_uiState.value.isBatchModeActive) {
                    Log.d("ScannerVM", "Collected result from flow: $result")
                    _uiState.update { it.copy(lastResult = result) }
                    // Also update the instant overlay state
                    _scanResultUiState.update { 
                        it.copy(
                            result = result,
                            isFavorite = result.isFavorite,
                            customName = result.customName ?: context.getString(com.scannerpro.lectorqr.util.BarcodeTypeUtils.getTypeNameRes(result.type)),
                            renameInput = result.customName ?: context.getString(com.scannerpro.lectorqr.util.BarcodeTypeUtils.getTypeNameRes(result.type)),
                            isLoading = false
                        )
                    }
                } else {
                    _scanResultUiState.update { it.copy(result = result) }
                }
                
                // Interstitial Logic
                scanCount++
                if (scanCount % 3 == 0) {
                    viewModelScope.launch {
                        if (!_uiState.value.isPremium) {
                            _interstitialTrigger.emit(Unit)
                        }
                    }
                }
            }
        }
    }

    fun handleBarcode(barcode: com.google.mlkit.vision.barcode.common.Barcode, bitmap: android.graphics.Bitmap?) {
        viewModelScope.launch {
            val keepDuplicates = settingsRepository.isKeepDuplicatesEnabled.firstOrNull() ?: true
            if (!keepDuplicates) {
                // Check if already exists in history (approximate check based on rawValue and recent timestamp)
                // This is a simple implementation. A better one would be in the repository or DAO.
                // For now, let's just proceed or skip if it's the exact same as lastResult
                if (_uiState.value.lastResult?.rawValue == barcode.rawValue) {
                    return@launch
                }
            }

            val isAddToHistory = settingsRepository.isAddToHistoryEnabled.firstOrNull() ?: true
            if (isAddToHistory) {
                repository.onBarcodeDetected(barcode, bitmap)
            } else if (!_uiState.value.isBatchModeActive) {
                // If history is disabled and NOT in batch mode, we still want to show the result overlay
                val result = repository.processBarcodeManually(barcode, bitmap)
                if (result != null) {
                    _scanResultUiState.update { 
                        it.copy(
                            result = result,
                            isFavorite = result.isFavorite,
                            customName = result.customName ?: "Texto",
                            renameInput = result.customName ?: "Texto",
                            isLoading = false
                        )
                    }
                }
            }
            
            if (_uiState.value.isBatchModeActive) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    val scannedLabel = context.getString(R.string.scanned_label)
                    val codeLabel = context.getString(R.string.type_text)
                    android.widget.Toast.makeText(context, "$scannedLabel ${barcode.displayValue ?: codeLabel}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            // Trigger feedback
            if (settingsRepository.isBeepEnabled.firstOrNull() == true) {
                playBeep()
            }
            if (settingsRepository.isVibrateEnabled.firstOrNull() == true) {
                vibrate()
            }
            
            // Copy to clipboard if enabled
            if (settingsRepository.isCopyToClipboardEnabled.firstOrNull() == true) {
                val formattedValue = BarcodeTypeUtils.getFormattedValue(context, barcode.valueType, barcode.rawValue)
                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("QR Code", formattedValue))
            }
            
            // Open URL automatically if enabled
            if (settingsRepository.isOpenUrlAutomaticallyEnabled.firstOrNull() == true) {
                barcode.url?.url?.let { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun playBeep() {
        try {
            val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e("ScannerVM", "Error playing beep", e)
        }
    }

    private fun vibrate() {
        try {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.e("ScannerVM", "Error vibrating", e)
        }
    }

    fun toggleFavorite() {
        val currentResult = _scanResultUiState.value.result ?: return
        val newFavoriteStatus = !currentResult.isFavorite
        viewModelScope.launch {
            toggleFavoriteUseCase(currentResult.id, newFavoriteStatus)
            _scanResultUiState.update { 
                it.copy(
                    isFavorite = newFavoriteStatus,
                    result = currentResult.copy(isFavorite = newFavoriteStatus)
                ) 
            }
        }
    }

    fun openRenameDialog() {
        _scanResultUiState.update { it.copy(isRenameDialogOpen = true) }
    }

    fun closeRenameDialog() {
        _scanResultUiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun updateRenameInput(name: String) {
        _scanResultUiState.update { it.copy(renameInput = name) }
    }

    fun saveName() {
        val currentResult = _scanResultUiState.value.result ?: return
        val newName = _scanResultUiState.value.renameInput
        viewModelScope.launch {
            updateScanNameUseCase(currentResult.id, newName)
            _scanResultUiState.update { 
                it.copy(
                    isRenameDialogOpen = false,
                    customName = newName,
                    result = currentResult.copy(customName = newName)
                ) 
            }
        }
    }

    fun scanFromGallery(uri: Uri) {
        android.util.Log.e("ScannerVM", "scanFromGallery called with uri: $uri")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // The repository emits to flow, but we also capture the result here for instant manual update
            try {
                val result = repository.processImageFromGallery(uri)
                android.util.Log.e("ScannerVM", "scanFromGallery repository returned: $result")
                if (result != null) {
                    _scanResultUiState.update { 
                        it.copy(
                            result = result,
                            isFavorite = result.isFavorite,
                            customName = result.customName ?: "Texto",
                            renameInput = result.customName ?: "Texto",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScannerVM", "Error in scanFromGallery", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashEnabled = !it.isFlashEnabled) }
    }

    fun flipCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun onZoomChanged(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
    }

    fun onZoomRangeChanged(min: Float, max: Float) {
        _uiState.update { it.copy(minZoomRatio = min, maxZoomRatio = max) }
    }

    fun requestGalleryPicker() {
        _uiState.update { it.copy(isGalleryRequested = true) }
    }

    fun onGalleryPickerLaunched() {
        _uiState.update { it.copy(isGalleryRequested = false) }
    }

    fun onResultHandled() {
        _uiState.update { it.copy(lastResult = null) }
        _scanResultUiState.update { ScanResultUiState() } // Reset overlay state
    }

    fun isProfileScan(id: Long): Boolean {
        val prefs = context.getSharedPreferences("qr_profile", android.content.Context.MODE_PRIVATE)
        return prefs.getLong("profileScanId", -1L) == id
    }

    fun deleteScan() {
        val currentResult = _scanResultUiState.value.result ?: return
        viewModelScope.launch {
            deleteScanUseCase(currentResult.id)
            onResultHandled()
        }
    }

    fun exportAsTxt(isShare: Boolean = false) {
        val result = _scanResultUiState.value.result ?: return
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, result.type, result.rawValue)
        val content = """
            ${context.getString(R.string.export_name_label)} ${_scanResultUiState.value.customName}
            ${context.getString(R.string.export_content_label)} $formattedValue
            ${context.getString(R.string.export_date_label)} ${java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))}
        """.trimIndent()
        val filename = "${_scanResultUiState.value.customName}.txt"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportAsCsv(isShare: Boolean = false) {
        val result = _scanResultUiState.value.result ?: return
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, result.type, result.rawValue).replace("\n", " ").replace("\"", "\"\"")
        val content = "${context.getString(R.string.csv_header_name)},${context.getString(R.string.csv_header_content)},${context.getString(R.string.csv_header_date)}\n" +
                "\"${_scanResultUiState.value.customName}\",\"$formattedValue\",\"${result.timestamp}\""
        val filename = "${_scanResultUiState.value.customName}.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", content)
        }
    }


    fun toggleBatchMode() {
        _uiState.update { it.copy(isBatchModeActive = !it.isBatchModeActive) }
    }

    fun getSearchUrl(query: String): String {
        return when (_scanResultUiState.value.searchEngine) {
            "Bing" -> "https://www.bing.com/search?q=$query"
            "Yahoo" -> "https://search.yahoo.com/search?p=$query"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$query"
            "Yandex" -> "https://yandex.com/search/?text=$query"
            else -> "https://www.google.com/search?q=$query"
        }
    }

    fun saveQrToGallery() {
        viewModelScope.launch {
            val bitmap = _scanResultUiState.value.qrBitmap ?: _scanResultUiState.value.result?.imagePath?.let {
                try {
                    android.graphics.BitmapFactory.decodeFile(it)
                } catch (e: Exception) {
                    null
                }
            } ?: return@launch
            
            try {
                val filename = "QR_${_scanResultUiState.value.customName}_${System.currentTimeMillis()}.png"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Código guardado en Galería", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerVM", "Error saving", e)
            }
        }
    }

    fun shareApp() {
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                context.getString(R.string.share_app_text, context.packageName)
            )
            type = "text/plain"
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, context.getString(R.string.drawer_share))
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun openDeveloperPage() {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://play.google.com/store/apps/developer?id=ATP+Dev")
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun removeAds() {
        viewModelScope.launch {
            android.widget.Toast.makeText(context, context.getString(R.string.premium_soon), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
