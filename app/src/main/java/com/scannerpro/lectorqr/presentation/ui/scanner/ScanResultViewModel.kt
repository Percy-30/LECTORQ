package com.scannerpro.lectorqr.presentation.ui.scanner

import com.scannerpro.lectorqr.R
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
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import com.scannerpro.lectorqr.util.FileUtils
import javax.inject.Inject

data class ScanResultUiState(
    val result: BarcodeResult? = null,
    val isFavorite: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val renameInput: String = "",
    val customName: String = "",
    val isLoading: Boolean = false,
    val searchEngine: String = "Google",
    val isAppBrowserEnabled: Boolean = true,
    val qrBitmap: android.graphics.Bitmap? = null
)

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getScanByIdUseCase: GetScanByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateScanNameUseCase: UpdateScanNameUseCase,
    private val deleteScanUseCase: com.scannerpro.lectorqr.domain.usecase.DeleteScanUseCase,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository,
    private val generateQrUseCase: com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
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
                if (result.format == 256) { // QR_CODE
                    generateQr(result)
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

    fun prepareEditAndGetRoute(): String? {
        val result = _uiState.value.result ?: return null
        val prefsName = when (result.type) {
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_URL -> {
                val url = result.rawValue?.lowercase() ?: ""
                when {
                    url.startsWith("https://wa.me/") -> "qr_social_WhatsApp"
                    url.startsWith("https://instagram.com/") -> "qr_social_Instagram"
                    url.startsWith("https://facebook.com/") -> "qr_social_Facebook"
                    url.startsWith("https://youtube.com/") -> "qr_social_YouTube"
                    url.startsWith("https://twitter.com/") || url.startsWith("https://x.com/") -> "qr_social_Twitter"
                    url.startsWith("https://linkedin.com/") -> "qr_social_LinkedIn"
                    url.startsWith("https://tiktok.com/") -> "qr_social_TikTok"
                    else -> "qr_url"
                }
            }
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_TEXT -> {
                if (result.format == com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE) {
                    "qr_text"
                } else {
                    "qr_barcode"
                }
            }
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_SMS -> "qr_sms"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_EMAIL -> "qr_email"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_WIFI -> "qr_wifi"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_GEO -> "qr_location"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_CALENDAR_EVENT -> "qr_calendar"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_PHONE -> "qr_phone"
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_CONTACT_INFO -> {
                val profileId = context.getSharedPreferences("qr_profile", android.content.Context.MODE_PRIVATE).getLong("profileScanId", -1L)
                if (result.id == profileId) "qr_profile" else "qr_contact"
            }
            else -> {
                if (result.format != com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE) "qr_barcode" else return null
            }
        }

        val prefsToUse = if (prefsName.startsWith("qr_social")) "qr_social" else prefsName
        val prefs = context.getSharedPreferences(prefsToUse, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("title", result.customName)
            putInt("foregroundColor", result.foregroundColor ?: android.graphics.Color.BLACK)
            putInt("backgroundColor", result.backgroundColor ?: android.graphics.Color.WHITE)
            putString("rawValue", result.rawValue ?: "")
            when (prefsName) {
                "qr_url" -> putString("url", result.rawValue ?: "")
                "qr_text" -> putString("text", result.rawValue ?: "")
                "qr_phone" -> putString("phone", result.displayValue ?: "")
                "qr_sms" -> putString("phone", result.displayValue ?: "") // Partial restore
                "qr_email" -> putString("email", result.displayValue ?: "") // Partial restore
                "qr_barcode" -> putString("inputValue", result.rawValue ?: "")
                "qr_social_WhatsApp" -> putString("inputValue", result.rawValue?.removePrefix("https://wa.me/")?.removePrefix("+")?.replace(" ", "") ?: "")
                "qr_social_Instagram" -> putString("inputValue", result.rawValue?.removePrefix("https://instagram.com/") ?: "")
                "qr_social_Facebook" -> putString("inputValue", result.rawValue?.removePrefix("https://facebook.com/")?.removePrefix("https://www.facebook.com/") ?: "")
                "qr_social_YouTube" -> putString("inputValue", result.rawValue?.removePrefix("https://youtube.com/channel/")?.removePrefix("https://youtube.com/@")?.removePrefix("https://www.youtube.com/channel/")?.removePrefix("https://www.youtube.com/@")?.removePrefix("https://youtu.be/") ?: "")
                "qr_social_Twitter" -> putString("inputValue", result.rawValue?.removePrefix("https://twitter.com/")?.removePrefix("https://x.com/")?.removePrefix("https://www.twitter.com/") ?: "")
                "qr_social_LinkedIn" -> putString("inputValue", result.rawValue?.removePrefix("https://linkedin.com/in/")?.removePrefix("https://linkedin.com/")?.removePrefix("https://www.linkedin.com/in/") ?: "")
                "qr_social_TikTok" -> putString("inputValue", result.rawValue?.removePrefix("https://www.tiktok.com/@")?.removePrefix("https://tiktok.com/@") ?: "")
            }
            if (prefsName == "qr_barcode") {
                putInt("format", result.format)
            }
            apply()
        }

        return when (prefsName) {
            "qr_url" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateUrl.route
            "qr_text" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateText.route
            "qr_sms" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateSms.route
            "qr_email" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateEmail.route
            "qr_wifi" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateWifi.route
            "qr_location" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateLocation.route
            "qr_calendar" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateCalendar.route
            "qr_phone" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreatePhone.route
            "qr_contact" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateContact.route
            "qr_profile" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateQr.route
            "qr_social_WhatsApp" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateWhatsApp.route
            "qr_social_Instagram" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateInstagram.route
            "qr_social_Facebook" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateFacebook.route
            "qr_social_YouTube" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateYouTube.route
            "qr_social_Twitter" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateTwitter.route
            "qr_social_LinkedIn" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateLinkedIn.route
            "qr_social_TikTok" -> com.scannerpro.lectorqr.presentation.navigation.Screen.CreateTikTok.route
            else -> null
        }
    }

    fun exportAsTxt(isShare: Boolean = false) {
        val result = _uiState.value.result ?: return
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, result.type, result.rawValue)
        val content = """
            ${context.getString(R.string.export_name_label)} ${_uiState.value.customName}
            ${context.getString(R.string.export_content_label)} $formattedValue
            ${context.getString(R.string.export_date_label)} ${java.text.SimpleDateFormat("d MMM. yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(result.timestamp))}
        """.trimIndent()
        val filename = "${_uiState.value.customName}.txt"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportAsCsv(isShare: Boolean = false) {
        val result = _uiState.value.result ?: return
        val formattedValue = BarcodeTypeUtils.getFormattedValue(context, result.type, result.rawValue).replace("\n", " ").replace("\"", "\"\"")
        val content = "${context.getString(R.string.csv_header_name)},${context.getString(R.string.csv_header_content)},${context.getString(R.string.csv_header_date)}\n" +
                "\"${_uiState.value.customName}\",\"$formattedValue\",\"${result.timestamp}\""
        val filename = "${_uiState.value.customName}.csv"
        if (isShare) {
            FileUtils.shareFile(context, filename, "text/csv", content)
        } else {
            FileUtils.saveFileToDownloads(context, filename, "text/csv", content)
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

    private fun generateQr(result: BarcodeResult) {
        viewModelScope.launch {
            val logoBitmap = if (settingsRepository.isPremium.value) {
                 com.scannerpro.lectorqr.util.QrLogoHelper.getLogoForType(
                     context = context,
                     typeId = result.type,
                     content = result.rawValue,
                     foregroundColor = result.foregroundColor ?: android.graphics.Color.BLACK
                 )
            } else null

            val bitmap = generateQrUseCase(
                text = result.rawValue ?: "",
                foregroundColor = result.foregroundColor ?: android.graphics.Color.BLACK,
                backgroundColor = result.backgroundColor ?: android.graphics.Color.WHITE,
                logo = logoBitmap
            )
            _uiState.update { it.copy(qrBitmap = bitmap) }
        }
    }

    fun saveQrToGallery() {
        viewModelScope.launch {
            val bitmap = _uiState.value.qrBitmap ?: _uiState.value.result?.imagePath?.let {
                try {
                    android.graphics.BitmapFactory.decodeFile(it)
                } catch (e: Exception) {
                    null
                }
            } ?: return@launch
            
            try {
                val filename = "QR_${_uiState.value.customName}_${System.currentTimeMillis()}.png"
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
                android.util.Log.e("ScanResultVM", "Error saving", e)
            }
        }
    }
}
