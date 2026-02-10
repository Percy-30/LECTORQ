package com.scannerpro.lectorqr.presentation.ui.create.location

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

data class CreateLocationUiState(
    val latitude: String = "",
    val longitude: String = "",
    val query: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false
)

@HiltViewModel
class CreateLocationViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateLocationUiState())
    val uiState: StateFlow<CreateLocationUiState> = _uiState.asStateFlow()

    fun onLatitudeChanged(lat: String) = _uiState.update { it.copy(latitude = lat) }
    fun onLongitudeChanged(lng: String) = _uiState.update { it.copy(longitude = lng) }
    fun onQueryChanged(query: String) = _uiState.update { it.copy(query = query) }

    fun generateQr() {
        val lat = _uiState.value.latitude.trim()
        val lng = _uiState.value.longitude.trim()
        
        if (lat.isBlank() || lng.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val geoUrl = if (_uiState.value.query.isNotBlank()) {
                "geo:$lat,$lng?q=${_uiState.value.query}"
            } else {
                "geo:$lat,$lng"
            }
            
            val bitmap = generateQrUseCase(geoUrl)
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
                val filename = "QR_Location_${System.currentTimeMillis()}.png"
                
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
                android.util.Log.e("CreateLocationVM", "Error saving", e)
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

            context.startActivity(android.content.Intent.createChooser(intent, "Compartir QR").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.util.Log.e("CreateLocationVM", "Error sharing", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocationScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else "Coordenadas geográficas", color = MaterialTheme.colorScheme.onPrimary) },
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
                onEditName = { /* Show Rename Dialog if needed */ },
                content = listOf(
                    "Latitud: ${uiState.latitude}",
                    "Longitud: ${uiState.longitude}",
                    if (uiState.query.isNotBlank()) "Consulta: ${uiState.query}" else ""
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
                    "Crea un código QR de ubicación",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.latitude,
                    onValueChange = { viewModel.onLatitudeChanged(it) },
                    label = { Text("Latitud") },
                    placeholder = { Text("-12.0464") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                OutlinedTextField(
                    value = uiState.longitude,
                    onValueChange = { viewModel.onLongitudeChanged(it) },
                    label = { Text("Longitud") },
                    placeholder = { Text("-77.0428") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    label = { Text("Consulta (opcional)") },
                    placeholder = { Text("Nombre del lugar") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Button(
                    onClick = { viewModel.generateQr() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = uiState.latitude.isNotBlank() && uiState.longitude.isNotBlank()
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
