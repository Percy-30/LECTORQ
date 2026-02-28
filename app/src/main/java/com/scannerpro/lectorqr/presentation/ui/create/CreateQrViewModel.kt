package com.scannerpro.lectorqr.presentation.ui.create

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateQrUiState(
    val fullName: String = "",
    val organization: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val title: String = "Mi código QR",
    val isFavorite: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val foregroundColor: Int = android.graphics.Color.BLACK,
    val backgroundColor: Int = android.graphics.Color.WHITE
)

@HiltViewModel
class CreateQrViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase,
    private val historyRepository: com.scannerpro.lectorqr.domain.repository.IHistoryRepository,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository,
    private val fileHelper: com.scannerpro.lectorqr.util.FileHelper
) : ViewModel() {

    private val prefs = context.getSharedPreferences("qr_profile", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CreateQrUiState())
    val uiState: StateFlow<CreateQrUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        if (_uiState.value.fullName.isNotBlank() || _uiState.value.phone.isNotBlank()) {
            generateVCardQr()
        }
    }


    private fun loadProfile() {
        _uiState.update { it.copy(
            fullName = prefs.getString("name", "") ?: "",
            organization = prefs.getString("org", "") ?: "",
            address = prefs.getString("addr", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            notes = prefs.getString("notes", "") ?: "",
            title = prefs.getString("title", "Mi código QR") ?: "Mi código QR",
            isFavorite = prefs.getBoolean("isFavorite", false),
            foregroundColor = prefs.getInt("foregroundColor", android.graphics.Color.BLACK),
            backgroundColor = prefs.getInt("backgroundColor", android.graphics.Color.WHITE)
        ) }
    }

    private fun saveProfile() {
        prefs.edit().apply {
            putString("name", _uiState.value.fullName)
            putString("org", _uiState.value.organization)
            putString("addr", _uiState.value.address)
            putString("phone", _uiState.value.phone)
            putString("email", _uiState.value.email)
            putString("notes", _uiState.value.notes)
            putString("title", _uiState.value.title)
            putBoolean("isFavorite", _uiState.value.isFavorite)
            putInt("foregroundColor", _uiState.value.foregroundColor)
            putInt("backgroundColor", _uiState.value.backgroundColor)
            apply()
        }
    }

    fun onFullNameChanged(it: String) = _uiState.update { state -> state.copy(fullName = it) }
    fun onOrganizationChanged(it: String) = _uiState.update { state -> state.copy(organization = it) }
    fun onAddressChanged(it: String) = _uiState.update { state -> state.copy(address = it) }
    fun onPhoneChanged(it: String) = _uiState.update { state -> state.copy(phone = it) }
    fun onEmailChanged(it: String) = _uiState.update { state -> state.copy(email = it) }
    fun onNotesChanged(it: String) = _uiState.update { state -> state.copy(notes = it) }
    fun onForegroundColorChanged(it: Int) = _uiState.update { state -> state.copy(foregroundColor = it) }
    fun onBackgroundColorChanged(it: Int) = _uiState.update { state -> state.copy(backgroundColor = it) }

    fun toggleFavorite() {
        val newState = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newState) }
        saveProfile()
        syncWithDatabase()
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        saveProfile()
        if (_uiState.value.isFavorite) {
            syncWithDatabase()
        }
    }

    fun deleteQr() {
        if (_uiState.value.isFavorite) {
            viewModelScope.launch {
                val scanId = prefs.getLong("profileScanId", -1L)
                if (scanId != -1L) {
                    historyRepository.deleteScan(scanId)
                    prefs.edit().putLong("profileScanId", -1L).apply()
                }
            }
        }
        _uiState.update { it.copy(showResult = false, isFavorite = false, fullName = "", organization = "", address = "", phone = "", email = "", notes = "", qrBitmap = null) }
        saveProfile()
    }

    private fun syncWithDatabase() {
        viewModelScope.launch {
            val state = _uiState.value
            val scanId = prefs.getLong("profileScanId", -1L)
            
            if (state.isFavorite) {
                // Construct vCard string
                val vCard = StringBuilder().apply {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    append("FN:${state.fullName}\n")
                    append("ORG:${state.organization}\n")
                    append("ADR:;;${state.address}\n")
                    append("TEL:${state.phone}\n")
                    append("EMAIL:${state.email}\n")
                    append("NOTE:${state.notes}\n")
                    append("END:VCARD")
                }.toString()

                val imagePath = if (state.qrBitmap != null) {
                    fileHelper.saveBitmapToInternalStorage(context, state.qrBitmap!!, "PROFILE_${System.currentTimeMillis()}")
                } else null

                val barcodeResult = com.scannerpro.lectorqr.domain.model.BarcodeResult(
                    id = if (scanId != -1L) scanId else 0L,
                    displayValue = state.fullName,
                    rawValue = vCard,
                    format = 256, // QR_CODE
                    type = com.google.mlkit.vision.barcode.common.Barcode.TYPE_CONTACT_INFO,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = true,
                    imagePath = imagePath,
                    customName = state.title,
                    foregroundColor = state.foregroundColor,
                    backgroundColor = state.backgroundColor
                )

                if (scanId == -1L) {
                    val newId = historyRepository.insertScan(barcodeResult)
                    prefs.edit().putLong("profileScanId", newId).apply()
                } else {
                    historyRepository.insertScan(barcodeResult) // REPLACE strategy
                }
            } else if (scanId != -1L) {
                historyRepository.deleteScan(scanId)
                prefs.edit().putLong("profileScanId", -1L).apply()
            }
        }
    }

    fun generateVCardQr() {
        val state = _uiState.value
        if (state.fullName.isBlank() && state.phone.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveProfile()

            val vCard = StringBuilder().apply {
                append("BEGIN:VCARD\n")
                append("VERSION:3.0\n")
                append("FN:${state.fullName}\n")
                append("ORG:${state.organization}\n")
                append("ADR:;;${state.address}\n")
                append("TEL:${state.phone}\n")
                append("EMAIL:${state.email}\n")
                append("NOTE:${state.notes}\n")
                append("END:VCARD")
            }.toString()

            val logoBitmap = if (settingsRepository.isPremium.value) {
                com.scannerpro.lectorqr.util.BitmapUtils.getDrawableAsBitmap(context, com.scannerpro.lectorqr.R.drawable.ic_person, 100, _uiState.value.foregroundColor)
            } else null

            val bitmap = generateQrUseCase(
                text = vCard,
                foregroundColor = _uiState.value.foregroundColor,
                backgroundColor = _uiState.value.backgroundColor,
                logo = logoBitmap
            )
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
            
            if (_uiState.value.isFavorite) {
                syncWithDatabase()
            }
        }
    }

    fun shareQrCode() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch {
            try {
                val file = android.util.Log.e("CreateQrVM", "Sharing QR...").run {
                    val cachePath = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val fileName = "QR_Profile_${System.currentTimeMillis()}.png"
                    val file = java.io.File(context.cacheDir, "images")
                    file.mkdirs()
                    val stream = java.io.FileOutputStream(java.io.File(file, fileName))
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        java.io.File(file, fileName)
                    )
                }

                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, file)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = android.content.Intent.createChooser(intent, "Compartir código QR")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                android.util.Log.e("CreateQrVM", "Error sharing QR", e)
            }
        }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val filename = "QR_Profile_${System.currentTimeMillis()}.jpg"
                    var fos: java.io.OutputStream? = null
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        context.contentResolver?.also { resolver ->
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                            val imageUri: android.net.Uri? = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            fos = imageUri?.let { resolver.openOutputStream(it) }
                            fos?.use {
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
                            }
                            imageUri?.let { uri ->
                                contentValues.clear()
                                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(uri, contentValues, null, null)
                            }
                        }
                    } else {
                        val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                        val image = java.io.File(imagesDir, filename)
                        fos = java.io.FileOutputStream(image)
                        fos?.use {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
                        }
                        // Refresh gallery for older versions
                        val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = android.net.Uri.fromFile(image)
                        context.sendBroadcast(mediaScanIntent)
                    }
                    true
                } catch (e: Exception) {
                    android.util.Log.e("CreateQrVM", "Error saving to gallery", e)
                    false
                }
            }
            if (result) {
                android.widget.Toast.makeText(context, "QR guardado en la galería", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Error al guardar el QR", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun backToEdit() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun exportToTxt(isShare: Boolean = true) {
        val state = _uiState.value
        val content = """
            Nombre: ${state.fullName}
            Organización: ${state.organization}
            Dirección: ${state.address}
            Teléfono: ${state.phone}
            Email: ${state.email}
            Notas: ${state.notes}
        """.trimIndent()
        val filename = "${state.title}.txt"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportToCsv(isShare: Boolean = true) {
        val state = _uiState.value
        val header = "Nombre,Organización,Dirección,Teléfono,Email,Notas\n"
        val row = "\"${state.fullName}\",\"${state.organization}\",\"${state.address}\",\"${state.phone}\",\"${state.email}\",\"${state.notes.replace("\"", "\"\"")}\""
        val filename = "${state.title}.csv"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/csv", header + row)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/csv", header + row)
        }
    }
}
