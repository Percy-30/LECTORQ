package com.scannerpro.lectorqr.presentation.ui.create.url

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

data class CreateUrlUiState(
    val url: String = "",
    val title: String = "URL",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false
)

@HiltViewModel
class CreateUrlViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase,
    private val historyRepository: com.scannerpro.lectorqr.domain.repository.IHistoryRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("qr_url", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(CreateUrlUiState())
    val uiState: StateFlow<CreateUrlUiState> = _uiState.asStateFlow()

    init {
        loadUrl()
    }

    private fun loadUrl() {
        _uiState.update { it.copy(
            url = prefs.getString("url", "") ?: "",
            title = prefs.getString("title", "URL") ?: "URL"
        ) }
    }

    private fun saveUrl() {
        prefs.edit().apply {
            putString("url", _uiState.value.url)
            putString("title", _uiState.value.title)
            apply()
        }
    }

    fun onUrlChanged(url: String) = _uiState.update { it.copy(url = url) }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        saveUrl()
    }

    fun generateQr() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveUrl()

            val bitmap = generateQrUseCase(url)
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
        }
    }

    fun backToEdit() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun deleteQr() {
        _uiState.update { it.copy(showResult = false, url = "", qrBitmap = null) }
        prefs.edit().clear().apply()
    }

    fun shareQr() {
        val bitmap = _uiState.value.qrBitmap ?: return
        try {
            val cachePath = java.io.File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "shared_qr.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(android.content.Intent.createChooser(intent, "Compartir QR").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("CreateUrlVM", "Error sharing QR", e)
        }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = "QR_${_uiState.value.title}_${System.currentTimeMillis()}.png"
                
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
                            android.widget.Toast.makeText(context, "QR guardado en Galería", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val file = java.io.File(picturesDir, filename)
                    java.io.FileOutputStream(file).use { stream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "QR guardado en Galería", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateUrlVM", "Error saving to gallery", e)
            }
        }
    }
}
