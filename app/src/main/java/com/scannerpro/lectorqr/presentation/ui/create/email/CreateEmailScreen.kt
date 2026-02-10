package com.scannerpro.lectorqr.presentation.ui.create.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateEmailUiState(
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false
)

@HiltViewModel
class CreateEmailViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEmailUiState())
    val uiState: StateFlow<CreateEmailUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email) }
    fun onSubjectChanged(subject: String) = _uiState.update { it.copy(subject = subject) }
    fun onMessageChanged(message: String) = _uiState.update { it.copy(message = message) }

    fun generateQr() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val mailtoUrl = buildString {
                append("mailto:$email")
                val subject = _uiState.value.subject.trim()
                val message = _uiState.value.message.trim()
                if (subject.isNotEmpty() || message.isNotEmpty()) {
                    append("?")
                    if (subject.isNotEmpty()) append("subject=${android.net.Uri.encode(subject)}")
                    if (subject.isNotEmpty() && message.isNotEmpty()) append("&")
                    if (message.isNotEmpty()) append("body=${android.net.Uri.encode(message)}")
                }
            }
            val bitmap = generateQrUseCase(mailtoUrl)
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
        }
    }

    fun backToEdit() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = "QR_Email_${System.currentTimeMillis()}.png"
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
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateEmailVM", "Error saving", e)
            }
        }
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

            val chooser = android.content.Intent.createChooser(intent, "Compartir QR")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("CreateEmailVM", "Error sharing", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEmailScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateEmailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else "Email", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = if (uiState.showResult) ({ viewModel.backToEdit() }) else onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (uiState.showResult) {
                        IconButton(onClick = { /* Menu */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.showResult && uiState.qrBitmap != null) {
            com.scannerpro.lectorqr.presentation.ui.create.components.StandardResultView(
                paddingValues = paddingValues,
                title = "Mi código",
                qrBitmap = uiState.qrBitmap!!,
                onSave = { viewModel.saveToGallery() },
                onShare = { viewModel.shareQr() },
                onEditName = { /* Rename Dialog */ },
                content = listOf(
                    "Email: ${uiState.email}",
                    if (uiState.subject.isNotBlank()) "Asunto: ${uiState.subject}" else "",
                    if (uiState.message.isNotBlank()) "Mensaje: ${uiState.message}" else ""
                )
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Crea un código QR para Email",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email") },
                    placeholder = { Text("correo@ejemplo.com") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                OutlinedTextField(
                    value = uiState.subject,
                    onValueChange = { viewModel.onSubjectChanged(it) },
                    label = { Text("Asunto (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = uiState.message,
                    onValueChange = { viewModel.onMessageChanged(it) },
                    label = { Text("Mensaje (opcional)") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 6
                )

                Button(
                    onClick = { viewModel.generateQr() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = uiState.email.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generar código QR", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
