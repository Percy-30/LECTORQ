package com.scannerpro.lectorqr.presentation.ui.create.url

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUrlScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateUrlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitle by remember(uiState.title) { mutableStateOf(uiState.title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showResult) uiState.title else "URL", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = if (uiState.showResult) ({ viewModel.backToEdit() }) else onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                        title = uiState.title,
                        qrBitmap = uiState.qrBitmap!!,
                        onSave = { viewModel.saveToGallery() },
                        onShare = { viewModel.shareQr() },
                        onEditName = { showRenameDialog = true },
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        isFavorite = uiState.isFavorite,
                        onExportTxt = { viewModel.exportToTxt() },
                        onExportCsv = { viewModel.exportToCsv() },
                        qrBackgroundColor = uiState.backgroundColor,
                        icon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) },
                        content = listOf("URL: ${uiState.url}")
                    )
                } else {
                    // Form View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Crea un código QR para una URL",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.url,
                            onValueChange = { viewModel.onUrlChanged(it) },
                            label = { Text("URL") },
                            placeholder = { Text("https://ejemplo.com") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            onClick = { viewModel.generateQr() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = uiState.url.isNotBlank()
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
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(newTitle)
                    showRenameDialog = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("CANCELAR", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
