package com.scannerpro.lectorqr.presentation.ui.create.wifi

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

data class CreateWifiUiState(
    val ssid: String = "",
    val password: String = "",
    val securityType: String = "WPA",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val showPassword: Boolean = false
)

@HiltViewModel
class CreateWifiViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateWifiUiState())
    val uiState: StateFlow<CreateWifiUiState> = _uiState.asStateFlow()

    fun onSsidChanged(ssid: String) = _uiState.update { it.copy(ssid = ssid) }
    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password) }
    fun onSecurityTypeChanged(type: String) = _uiState.update { it.copy(securityType = type) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun generateQr() {
        val ssid = _uiState.value.ssid.trim()
        if (ssid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val wifiString = buildString {
                append("WIFI:")
                append("S:${escape(ssid)};")
                append("T:${_uiState.value.securityType};")
                if (_uiState.value.password.isNotEmpty()) {
                    append("P:${escape(_uiState.value.password)};")
                }
                append(";")
            }
            val bitmap = generateQrUseCase(wifiString)
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
        }
    }

    private fun escape(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }

    fun backToEdit() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = "QR_WiFi_${System.currentTimeMillis()}.png"
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
                android.util.Log.e("CreateWifiVM", "Error saving", e)
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
            android.util.Log.e("CreateWifiVM", "Error sharing", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWifiScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateWifiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else "Wi-Fi", color = MaterialTheme.colorScheme.onPrimary) },
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
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.showResult && uiState.qrBitmap != null) {
                    com.scannerpro.lectorqr.presentation.ui.create.components.StandardResultView(
                        paddingValues = PaddingValues(0.dp),
                        title = "Mi código",
                        qrBitmap = uiState.qrBitmap!!,
                        onSave = { viewModel.saveToGallery() },
                        onShare = { viewModel.shareQr() },
                        onEditName = { /* Rename Dialog */ },
                        content = listOf(
                            "Red: ${uiState.ssid}",
                            "Seguridad: ${uiState.securityType}"
                        )
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Crea un código QR para Wi-Fi",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.ssid,
                            onValueChange = { viewModel.onSsidChanged(it) },
                            label = { Text("Nombre de red (SSID)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.securityType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de seguridad") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                listOf("WPA", "WEP", "None").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            viewModel.onSecurityTypeChanged(type)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChanged(it) },
                            label = { Text("Contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            visualTransformation = if (uiState.showPassword) 
                                androidx.compose.ui.text.input.VisualTransformation.None 
                            else 
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                    Icon(
                                        if (uiState.showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Button(
                            onClick = { viewModel.generateQr() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = uiState.ssid.isNotBlank()
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
            
            // Banner Ad at the bottom
            com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
