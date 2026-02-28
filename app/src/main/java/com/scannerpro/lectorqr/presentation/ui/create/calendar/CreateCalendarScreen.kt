package com.scannerpro.lectorqr.presentation.ui.create.calendar

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CreateCalendarUiState(
    val title: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val isAllDay: Boolean = false,
    val location: String = "",
    val description: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val customName: String = "Calendario",
    val foregroundColor: Int = android.graphics.Color.BLACK,
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val scanId: Long = -1L,
    val isFavorite: Boolean = false
)

@HiltViewModel
class CreateCalendarViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val generateQrUseCase: GenerateQrUseCase,
    private val historyRepository: com.scannerpro.lectorqr.domain.repository.IHistoryRepository,
    private val settingsRepository: com.scannerpro.lectorqr.domain.repository.ISettingsRepository,
    private val fileHelper: com.scannerpro.lectorqr.util.FileHelper
) : ViewModel() {

    private val prefs = context.getSharedPreferences("qr_calendar", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(CreateCalendarUiState())
    val uiState: StateFlow<CreateCalendarUiState> = _uiState.asStateFlow()

    init {
        loadDraft()
    }

    private fun loadDraft() {
        val draftRaw = prefs.getString("rawValue", "") ?: ""
        val draftTitle = prefs.getString("title", "Calendario") ?: "Calendario"
        val fgColor = prefs.getInt("foregroundColor", android.graphics.Color.BLACK)
        val bgColor = prefs.getInt("backgroundColor", android.graphics.Color.WHITE)
        
        if (draftRaw.isNotEmpty() && draftRaw.startsWith("BEGIN:VEVENT")) {
            val summary = draftRaw.substringAfter("SUMMARY:", "").substringBefore("\n").trim()
            val location = draftRaw.substringAfter("LOCATION:", "").substringBefore("\n").trim()
            val description = draftRaw.substringAfter("DESCRIPTION:", "").substringBefore("\n").trim()
            
            _uiState.update { it.copy(
                title = summary,
                location = location,
                description = description,
                customName = draftTitle,
                foregroundColor = fgColor,
                backgroundColor = bgColor
            ) }
            prefs.edit().clear().apply()
        }
    }

    fun onTitleChanged(title: String) = _uiState.update { it.copy(title = title) }
    fun onLocationChanged(loc: String) = _uiState.update { it.copy(location = loc) }
    fun onDescriptionChanged(desc: String) = _uiState.update { it.copy(description = desc) }
    fun onAllDayChanged(allDay: Boolean) = _uiState.update { it.copy(isAllDay = allDay) }
    fun onStartDateChanged(date: Long) = _uiState.update { it.copy(startDate = date) }
    fun onEndDateChanged(date: Long) = _uiState.update { it.copy(endDate = date) }
    fun onForegroundColorChanged(color: Int) = _uiState.update { it.copy(foregroundColor = color) }
    fun onBackgroundColorChanged(color: Int) = _uiState.update { it.copy(backgroundColor = color) }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        syncWithHistory()
    }
    fun updateCustomName(name: String) = _uiState.update { it.copy(customName = name) }

    fun generateQr() {
        val title = _uiState.value.title.trim()
        if (title.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            val vEvent = buildString {
                appendLine("BEGIN:VCALENDAR")
                appendLine("VERSION:2.0")
                appendLine("BEGIN:VEVENT")
                appendLine("SUMMARY:$title")
                appendLine("DTSTART:${sdf.format(Date(_uiState.value.startDate))}")
                appendLine("DTEND:${sdf.format(Date(_uiState.value.endDate))}")
                if (_uiState.value.location.isNotBlank()) appendLine("LOCATION:${_uiState.value.location}")
                if (_uiState.value.description.isNotBlank()) appendLine("DESCRIPTION:${_uiState.value.description}")
                appendLine("END:VEVENT")
                appendLine("END:VCALENDAR")
            }
            
            val logoBitmap = if (settingsRepository.isPremium.value) {
                com.scannerpro.lectorqr.util.BitmapUtils.getDrawableAsBitmap(context, com.scannerpro.lectorqr.R.drawable.ic_calendar, 100, _uiState.value.foregroundColor)
            } else null
            
            val bitmap = generateQrUseCase(
                text = vEvent,
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
            if (state.title.isBlank()) return@launch
            
            val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            val vEvent = buildString {
                appendLine("BEGIN:VCALENDAR")
                appendLine("VERSION:2.0")
                appendLine("BEGIN:VEVENT")
                appendLine("SUMMARY:${state.title}")
                appendLine("DTSTART:${sdf.format(Date(state.startDate))}")
                appendLine("DTEND:${sdf.format(Date(state.endDate))}")
                if (state.location.isNotBlank()) appendLine("LOCATION:${state.location}")
                if (state.description.isNotBlank()) appendLine("DESCRIPTION:${state.description}")
                appendLine("END:VEVENT")
                appendLine("END:VCALENDAR")
            }
            
            val imagePath = if (state.qrBitmap != null) {
                fileHelper.saveBitmapToInternalStorage(context, state.qrBitmap!!, "CALENDAR_${System.currentTimeMillis()}")
            } else null

            val barcodeResult = com.scannerpro.lectorqr.domain.model.BarcodeResult(
                id = if (state.scanId != -1L) state.scanId else 0L,
                displayValue = state.title,
                rawValue = vEvent,
                format = 256, // QR_CODE
                type = com.google.mlkit.vision.barcode.common.Barcode.TYPE_CALENDAR_EVENT,
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

    fun backToEdit() = _uiState.update { it.copy(showResult = false) }

    fun deleteQr() {
        _uiState.update { it.copy(showResult = false, title = "", location = "", description = "", qrBitmap = null, scanId = -1L) }
    }

    fun exportToTxt(isShare: Boolean = true) {
        val title = _uiState.value.title
        if (title.isBlank()) return
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val calData = "Título: $title\nInicio: ${sdf.format(Date(_uiState.value.startDate))}\nFin: ${sdf.format(Date(_uiState.value.endDate))}\nUbicación: ${_uiState.value.location}\nDescripción: ${_uiState.value.description}"
        val filename = "${_uiState.value.customName}.txt"
        if (isShare) {
            com.scannerpro.lectorqr.util.FileUtils.shareFile(context, filename, "text/plain", calData)
        } else {
            com.scannerpro.lectorqr.util.FileUtils.saveFileToDownloads(context, filename, "text/plain", calData)
        }
    }

    fun exportToCsv(isShare: Boolean = true) {
        val title = _uiState.value.title
        if (title.isBlank()) return
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val header = "Título,Inicio,Fin,Ubicación,Descripción\n"
        val row = "\"$title\",\"${sdf.format(Date(_uiState.value.startDate))}\",\"${sdf.format(Date(_uiState.value.endDate))}\",\"${_uiState.value.location}\",\"${_uiState.value.description.replace("\"", "\"\"")}\""
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
                val filename = "QR_Calendar_${System.currentTimeMillis()}.png"
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
                android.util.Log.e("CreateCalendarVM", "Error saving", e)
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
            android.util.Log.e("CreateCalendarVM", "Error sharing", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCalendarScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("yyyy-MM-dd   HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) "Crear" else "Crear", color = MaterialTheme.colorScheme.onPrimary) },
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
                        IconButton(onClick = { viewModel.generateQr() }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirmar", tint = MaterialTheme.colorScheme.onPrimary)
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
                onFavoriteClick = { viewModel.toggleFavorite() },
                onExportTxt = { viewModel.exportToTxt() },
                onExportCsv = { viewModel.exportToCsv() },
                isFavorite = uiState.isFavorite,
                qrBackgroundColor = uiState.backgroundColor,
                icon = { Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) },
                content = listOf(
                    "Evento: ${uiState.title}",
                    "Ubicación: ${uiState.location}",
                    "Descripción: ${uiState.description}"
                )
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Calendario", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = { Text("Nombre del evento") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Column {
                    Text("Comienzo:", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(sdf.format(Date(uiState.startDate)), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Final:", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(sdf.format(Date(uiState.endDate)), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.isAllDay,
                        onCheckedChange = { viewModel.onAllDayChanged(it) },
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("Evento de todo el día", color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = { viewModel.onLocationChanged(it) },
                    label = { Text("Ubicación") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
