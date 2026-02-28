package com.scannerpro.lectorqr.presentation.ui.create.social

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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerpro.lectorqr.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
import com.scannerpro.lectorqr.domain.repository.IHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val inputValue: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val customName: String = "",
    val isFavorite: Boolean = false,
    val scanId: Long = -1L,
    val foregroundColor: Int = android.graphics.Color.BLACK,
    val backgroundColor: Int = android.graphics.Color.WHITE
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase,
    private val historyRepository: IHistoryRepository,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository,
    private val fileHelper: com.scannerpro.lectorqr.util.FileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("qr_social", android.content.Context.MODE_PRIVATE)

    init {
        loadDraft()
    }

    private fun loadDraft() {
        val draftInput = prefs.getString("inputValue", "") ?: ""
        val draftTitle = prefs.getString("title", "Social") ?: "Social"
        val fgColor = prefs.getInt("foregroundColor", android.graphics.Color.BLACK)
        val bgColor = prefs.getInt("backgroundColor", android.graphics.Color.WHITE)
        
        if (draftInput.isNotEmpty()) {
            _uiState.update { it.copy(
                inputValue = draftInput,
                customName = draftTitle,
                foregroundColor = fgColor,
                backgroundColor = bgColor
            ) }
            prefs.edit().clear().apply()
        }
    }

    fun onInputChanged(value: String) = _uiState.update { it.copy(inputValue = value) }
    fun onForegroundColorChanged(color: Int) = _uiState.update { it.copy(foregroundColor = color) }
    fun onBackgroundColorChanged(color: Int) = _uiState.update { it.copy(backgroundColor = color) }
    
    fun initType(type: String) {
        if (_uiState.value.customName.isBlank()) {
            _uiState.update { it.copy(customName = type) }
        }
    }

    fun generateQr(type: String) {
        val input = _uiState.value.inputValue.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val url = when (type) {
                "WhatsApp" -> "https://wa.me/${input.removePrefix("+").replace(" ", "")}"
                "Instagram" -> "https://instagram.com/${input.removePrefix("@")}"
                "Facebook" -> "https://facebook.com/$input"
                "YouTube" -> {
                    if (input.startsWith("UC") || input.startsWith("UU")) {
                        "https://youtube.com/channel/$input"
                    } else {
                        "https://youtube.com/@${input.removePrefix("@")}"
                    }
                }
                "Twitter" -> "https://twitter.com/${input.removePrefix("@")}"
                "LinkedIn" -> "https://linkedin.com/in/${input.removePrefix("in/")}"
                "TikTok" -> "https://www.tiktok.com/@${input.removePrefix("@")}"
                else -> input
            }
            
            val logoRes = when (type) {
                "WhatsApp" -> R.drawable.ic_whatsapp
                "Instagram" -> R.drawable.ic_instagram
                "Facebook" -> R.drawable.ic_facebook
                "YouTube" -> R.drawable.ic_youtube
                "Twitter" -> R.drawable.ic_twitter_x
                "LinkedIn" -> R.drawable.ic_linkedin
                "TikTok" -> R.drawable.ic_tiktok
                else -> null
            }
            
            val logoBitmap = if (settingsRepository.isPremium.value && logoRes != null) {
                com.scannerpro.lectorqr.util.BitmapUtils.getDrawableAsBitmap(context, logoRes, 100)
            } else null
            
            val bitmap = generateQrUseCase(
                text = url,
                foregroundColor = _uiState.value.foregroundColor,
                backgroundColor = _uiState.value.backgroundColor,
                logo = logoBitmap
            )
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false, showResult = true) }
            syncWithHistory(url)
        }
    }

    fun backToEdit() = _uiState.update { it.copy(showResult = false) }

    fun deleteQr() {
        _uiState.update { it.copy(showResult = false, inputValue = "", qrBitmap = null, scanId = -1L) }
    }

    fun exportToTxt(isShare: Boolean = true) {
        val input = _uiState.value.inputValue
        if (input.isBlank()) return
        val socialData = "Tipo: ${_uiState.value.customName}\nContenido: $input"
        val filename = "${_uiState.value.customName}.txt"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/plain", socialData)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/plain", socialData)
        }
    }

    fun exportToCsv(isShare: Boolean = true) {
        val input = _uiState.value.inputValue
        if (input.isBlank()) return
        val header = "Tipo,Contenido\n"
        val row = "\"${_uiState.value.customName}\",\"$input\""
        val filename = "${_uiState.value.customName}.csv"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/csv", header + row)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/csv", header + row)
        }
    }

    fun toggleFavorite(type: String) {
        val newState = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newState) }
        // We need the generated URL here too
        generateQr(type) // This will call syncWithHistory
    }

    private fun syncWithHistory(generatedUrl: String? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.inputValue.isBlank()) return@launch
            
            // Re-generate URL if not provided
            val url = generatedUrl ?: "" // This is simplified for favorite toggle
            
            val imagePath = if (state.qrBitmap != null) {
                fileHelper.saveBitmapToInternalStorage(context, state.qrBitmap!!, "SOCIAL_${System.currentTimeMillis()}")
            } else null

            val barcodeResult = com.scannerpro.lectorqr.domain.model.BarcodeResult(
                id = if (state.scanId != -1L) state.scanId else 0L,
                displayValue = state.inputValue,
                rawValue = url,
                format = 256, // QR_CODE
                type = com.google.mlkit.vision.barcode.common.Barcode.TYPE_URL,
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

    fun saveToGallery() {
        val bitmap = _uiState.value.qrBitmap ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
                            Toast.makeText(context, "QR guardado en Galería", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialVM", "Error saving", e)
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
            android.util.Log.e("SocialVM", "Error sharing", e)
        }
    }

    fun updateCustomName(name: String, type: String) {
        _uiState.update { it.copy(customName = name) }
        generateQr(type)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSocialScreen(
    type: String,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(type) {
        viewModel.initType(type)
    }

    val painter = when (type) {
        "WhatsApp" -> painterResource(id = R.drawable.ic_whatsapp)
        "Instagram" -> painterResource(id = R.drawable.ic_instagram)
        "Facebook" -> painterResource(id = R.drawable.ic_facebook)
        "YouTube" -> painterResource(id = R.drawable.ic_youtube)
        "Twitter" -> painterResource(id = R.drawable.ic_twitter_x)
        "LinkedIn" -> painterResource(id = R.drawable.ic_linkedin)
        "TikTok" -> painterResource(id = R.drawable.ic_tiktok)
        else -> rememberVectorPainter(Icons.Default.Share)
    }

    val label = when (type) {
        "WhatsApp" -> "Número de teléfono"
        "Instagram" -> "Nombre de usuario"
        "Facebook" -> "Nombre de usuario o ID"
        "YouTube" -> "Nombre del canal o handle"
        "Twitter" -> "Nombre de usuario o handle"
        "LinkedIn" -> "ID de perfil o nombre"
        else -> "Usuario"
    }

    val placeholder = when (type) {
        "WhatsApp" -> "+51 987 654 321"
        "Instagram" -> "@usuario"
        "Facebook" -> "usuario"
        "YouTube" -> "@canal"
        "Twitter" -> "@usuario"
        "LinkedIn" -> "nombre-usuario"
        else -> "usuario"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else type, color = MaterialTheme.colorScheme.onPrimary) },
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
        if (uiState.showResult && uiState.qrBitmap != null) {
            com.scannerpro.lectorqr.presentation.ui.create.components.StandardResultView(
                paddingValues = paddingValues,
                title = uiState.customName,
                qrBitmap = uiState.qrBitmap!!,
                onSave = { viewModel.saveToGallery() },
                onShare = { viewModel.shareQr() },
                onEditName = { showRenameDialog = true },
                isFavorite = uiState.isFavorite,
                onFavoriteClick = { viewModel.toggleFavorite(type) },
                onExportTxt = { viewModel.exportToTxt() },
                onExportCsv = { viewModel.exportToCsv() },
                qrBackgroundColor = uiState.backgroundColor,
                icon = {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                },
                content = listOf("$type: ${uiState.inputValue}")
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
                    "Crea un código QR para $type",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.inputValue,
                    onValueChange = { viewModel.onInputChanged(it) },
                    label = { Text(label) },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    keyboardOptions = if (type == "WhatsApp") {
                        KeyboardOptions(keyboardType = KeyboardType.Phone)
                    } else {
                        KeyboardOptions.Default
                    }
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
                    onClick = { viewModel.generateQr(type) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = uiState.inputValue.isNotBlank()
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
                    viewModel.updateCustomName(dialogTitle, type)
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
