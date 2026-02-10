package com.scannerpro.lectorqr.presentation.ui.create.barcode

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.usecase.GenerateBarcodeUseCase
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BarcodeUiState(
    val inputValue: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val customName: String = "",
    val isFavorite: Boolean = false,
    val scanId: Long = -1L,
    val format: Int = 0
)

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateBarcodeUseCase: GenerateBarcodeUseCase,
    private val historyRepository: IHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeUiState())
    val uiState: StateFlow<BarcodeUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) = _uiState.update { it.copy(inputValue = value) }
    
    fun initFormat(formatName: String, format: Int) {
        if (_uiState.value.customName.isBlank()) {
            _uiState.update { it.copy(customName = formatName, format = format) }
        }
    }

    fun generateBarcode() {
        val state = _uiState.value
        val input = state.inputValue.trim()
        val format = state.format
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // ZXing 1D barcodes need more width than height for better visibility
            // but we'll stick to 512x512 as baseline, ZXing adjusts internally.
            val bitmap = generateBarcodeUseCase(input, format, 1024, 512)
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
        }
    }

    fun backToEdit() = _uiState.update { it.copy(showResult = false) }

    fun toggleFavorite() {
        val newState = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newState) }
        syncWithHistory()
    }

    private fun syncWithHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isFavorite) {
                val barcodeResult = com.scannerpro.lectorqr.domain.model.BarcodeResult(
                    id = if (state.scanId != -1L) state.scanId else 0L,
                    displayValue = state.inputValue,
                    rawValue = state.inputValue,
                    format = state.format,
                    type = 0,   // TEXT
                    timestamp = System.currentTimeMillis(),
                    isFavorite = true,
                    customName = state.customName
                )
                val newId = historyRepository.insertScan(barcodeResult)
                _uiState.update { it.copy(scanId = newId) }
            } else if (state.scanId != -1L) {
                historyRepository.deleteScan(state.scanId)
                _uiState.update { it.copy(scanId = -1L) }
            }
        }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = "BARCODE_${_uiState.value.customName}_${System.currentTimeMillis()}.png"
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
                            Toast.makeText(context, "Código guardado en Galería", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BarcodeVM", "Error saving", e)
            }
        }
    }

    fun shareBarcode() {
        val bitmap = _uiState.value.qrBitmap ?: return
        try {
            val cachePath = java.io.File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "shared_barcode.png")
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

            val chooser = android.content.Intent.createChooser(intent, "Compartir")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("BarcodeVM", "Error sharing", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBarcodeScreen(
    formatName: String,
    format: Int,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: BarcodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(formatName) }

    LaunchedEffect(formatName, format) {
        viewModel.initFormat(formatName, format)
        newTitle = formatName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else formatName, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = if (uiState.showResult) ({ viewModel.backToEdit() }) else onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (!uiState.showResult) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = MaterialTheme.colorScheme.onPrimary)
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
                title = uiState.customName,
                qrBitmap = uiState.qrBitmap!!,
                onSave = { viewModel.saveToGallery() },
                onShare = { viewModel.shareBarcode() },
                onEditName = { showRenameDialog = true },
                isFavorite = uiState.isFavorite,
                onFavoriteClick = { viewModel.toggleFavorite() },
                content = listOf("$formatName: ${uiState.inputValue}")
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
                    "Crea un código de barras $formatName",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.inputValue,
                    onValueChange = { viewModel.onInputChanged(it) },
                    label = { Text("Contenido") },
                    placeholder = { 
                        Text(when(formatName) {
                            "EAN_13" -> "1234567890123"
                            "EAN_8" -> "12345670"
                            "UPC_A" -> "123456789012"
                            else -> "Ingresa el texto"
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ViewWeek, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = { viewModel.generateBarcode() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = uiState.inputValue.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generar código", fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.initFormat(newTitle, format)
                    showRenameDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
