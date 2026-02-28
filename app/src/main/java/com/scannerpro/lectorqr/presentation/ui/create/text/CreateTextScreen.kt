package com.scannerpro.lectorqr.presentation.ui.create.text

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

data class CreateTextUiState(
    val text: String = "",
    val title: String = "Texto",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val customName: String = "Texto",
    val foregroundColor: Int = android.graphics.Color.BLACK,
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val scanId: Long = -1L,
    val isFavorite: Boolean = false
)

@HiltViewModel
class CreateTextViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase,
    private val historyRepository: com.scannerpro.lectorqr.domain.repository.IHistoryRepository,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository,
    private val fileHelper: com.scannerpro.lectorqr.util.FileHelper
) : ViewModel() {

    private val prefs = context.getSharedPreferences("qr_text", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(CreateTextUiState())
    val uiState: StateFlow<CreateTextUiState> = _uiState.asStateFlow()

    init {
        loadDraft()
    }

    private fun loadDraft() {
        val draftText = prefs.getString("text", "") ?: ""
        val draftTitle = prefs.getString("title", "Texto") ?: "Texto"
        val fgColor = prefs.getInt("foregroundColor", android.graphics.Color.BLACK)
        val bgColor = prefs.getInt("backgroundColor", android.graphics.Color.WHITE)
        
        if (draftText.isNotEmpty()) {
            _uiState.update { it.copy(
                text = draftText,
                customName = draftTitle,
                foregroundColor = fgColor,
                backgroundColor = bgColor
            ) }
            prefs.edit().clear().apply()
        }
    }

    fun onTextChanged(text: String) = _uiState.update { it.copy(text = text) }
    fun onForegroundColorChanged(color: Int) = _uiState.update { it.copy(foregroundColor = color) }
    fun onBackgroundColorChanged(color: Int) = _uiState.update { it.copy(backgroundColor = color) }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        syncWithHistory()
    }

    fun generateQr() {
        val text = _uiState.value.text.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logoBitmap = if (settingsRepository.isPremium.value) {
                com.scannerpro.lectorqr.util.BitmapUtils.getDrawableAsBitmap(context, com.scannerpro.lectorqr.R.drawable.ic_text, 100, _uiState.value.foregroundColor)
            } else null

            val bitmap = generateQrUseCase(
                text = text,
                foregroundColor = _uiState.value.foregroundColor,
                backgroundColor = _uiState.value.backgroundColor,
                logo = logoBitmap
            )
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
            syncWithHistory()
        }
    }

    private fun syncWithHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.text.isBlank()) return@launch
            
            val imagePath = if (state.qrBitmap != null) {
                fileHelper.saveBitmapToInternalStorage(context, state.qrBitmap!!, "TEXT_${System.currentTimeMillis()}")
            } else null

            val barcodeResult = com.scannerpro.lectorqr.domain.model.BarcodeResult(
                id = if (state.scanId != -1L) state.scanId else 0L,
                displayValue = state.text,
                rawValue = state.text,
                format = 256, // QR_CODE
                type = com.google.mlkit.vision.barcode.common.Barcode.TYPE_TEXT,
                timestamp = System.currentTimeMillis(),
                isFavorite = state.isFavorite,
                imagePath = imagePath,
                customName = state.customName,
                foregroundColor = state.foregroundColor,
                backgroundColor = state.backgroundColor
            )
            val newId = historyRepository.insertScan(barcodeResult)
            _uiState.update { it.copy(scanId = newId) }
        }
    }

    fun backToEdit() {
        _uiState.update { it.copy(showResult = false) }
    }

    fun deleteQr() {
        _uiState.update { it.copy(showResult = false, text = "", qrBitmap = null, scanId = -1L) }
    }

    fun exportToTxt(isShare: Boolean = true) {
        val text = _uiState.value.text
        if (text.isBlank()) return
        val content = "Contenido: $text"
        val filename = "${_uiState.value.customName}.txt"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/plain", content)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/plain", content)
        }
    }

    fun exportToCsv(isShare: Boolean = true) {
        val text = _uiState.value.text
        if (text.isBlank()) return
        val header = "Tipo,Contenido\n"
        val row = "Texto,\"${text.replace("\"", "\"\"")}\""
        val filename = "${_uiState.value.customName}.csv"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/csv", header + row)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/csv", header + row)
        }
    }

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = "QR_Text_${System.currentTimeMillis()}.png"
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
                android.util.Log.e("CreateTextVM", "Error saving", e)
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
            android.util.Log.e("CreateTextVM", "Error sharing", e)
        }
    }

    fun updateCustomName(name: String) {
        _uiState.update { it.copy(customName = name) }
        syncWithHistory()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTextScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateTextViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else "Texto", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = if (uiState.showResult) ({ viewModel.backToEdit() }) else onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (uiState.showResult) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        var subMenu by remember { mutableStateOf<String?>(null) }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { 
                                showMenu = false 
                                subMenu = null
                            }
                        ) {
                            when (subMenu) {
                                null -> {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.deleteQr()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Renombrar") },
                                        onClick = {
                                            showMenu = false
                                            showRenameDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("TXT") },
                                        onClick = { subMenu = "TXT" },
                                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("CSV") },
                                        onClick = { subMenu = "CSV" },
                                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.backToEdit()
                                        },
                                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                                    )
                                }
                                "TXT" -> {
                                    DropdownMenuItem(
                                        text = { Text("Volver") },
                                        onClick = { subMenu = null },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Compartir") },
                                        onClick = {
                                            showMenu = false
                                            subMenu = null
                                            viewModel.exportToTxt(isShare = true)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Guardar") },
                                        onClick = {
                                            showMenu = false
                                            subMenu = null
                                            viewModel.exportToTxt(isShare = false)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                                    )
                                }
                                "CSV" -> {
                                    DropdownMenuItem(
                                        text = { Text("Volver") },
                                        onClick = { subMenu = null },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Compartir") },
                                        onClick = {
                                            showMenu = false
                                            subMenu = null
                                            viewModel.exportToCsv(isShare = true)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Guardar") },
                                        onClick = {
                                            showMenu = false
                                            subMenu = null
                                            viewModel.exportToCsv(isShare = false)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                                    )
                                }
                            }
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
                        paddingValues = paddingValues,
                        title = uiState.customName,
                        qrBitmap = uiState.qrBitmap!!,
                        onSave = { viewModel.saveToGallery() },
                        onShare = { viewModel.shareQr() },
                        onEditName = { showRenameDialog = true },
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onExportTxt = { viewModel.exportToTxt() },
                        onExportCsv = { viewModel.exportToCsv() },
                        isFavorite = uiState.isFavorite,
                        qrBackgroundColor = uiState.backgroundColor,
                        icon = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) },
                        content = listOf(uiState.text)
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
                            "Crea un código QR para texto",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.text,
                            onValueChange = { viewModel.onTextChanged(it) },
                            label = { Text("Texto") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            maxLines = 10
                        )

                        val isPremium = com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium.current
                        
                        com.scannerpro.lectorqr.presentation.ui.create.components.ColorPickerSection(
                            title = "Color de primer plano",
                            selectedColor = uiState.foregroundColor,
                            isPremium = isPremium,
                            onColorSelected = { viewModel.onForegroundColorChanged(it) }
                        )
                        
                        com.scannerpro.lectorqr.presentation.ui.create.components.ColorPickerSection(
                            title = "Color de fondo",
                            selectedColor = uiState.backgroundColor,
                            isPremium = isPremium,
                            onColorSelected = { viewModel.onBackgroundColorChanged(it) }
                        )

                        Button(
                            onClick = { viewModel.generateQr() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = uiState.text.isNotBlank()
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
    if (showRenameDialog) {
        var dialogTitle by remember(uiState.customName) { mutableStateOf(uiState.customName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar") },
            text = {
                OutlinedTextField(
                    value = dialogTitle,
                    onValueChange = { dialogTitle = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateCustomName(dialogTitle)
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
