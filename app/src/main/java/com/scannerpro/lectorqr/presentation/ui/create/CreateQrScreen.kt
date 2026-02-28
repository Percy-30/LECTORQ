package com.scannerpro.lectorqr.presentation.ui.create

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
fun CreateQrScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateQrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showRenameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newTitle by androidx.compose.runtime.remember(uiState.title) { androidx.compose.runtime.mutableStateOf(uiState.title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Crear", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (uiState.showResult) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        var subMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
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
                        IconButton(onClick = { /* Already in Profile */ }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = { viewModel.generateVCardQr() }) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Dark gray/black
    ) { paddingValues ->
        if (uiState.showResult && uiState.qrBitmap != null) {
            com.scannerpro.lectorqr.presentation.ui.create.components.StandardResultView(
                paddingValues = paddingValues,
                title = uiState.title,
                qrBitmap = uiState.qrBitmap!!,
                onSave = { viewModel.saveToGallery() },
                onShare = { viewModel.shareQrCode() },
                onEditName = { showRenameDialog = true },
                isFavorite = uiState.isFavorite,
                onFavoriteClick = { viewModel.toggleFavorite() },
                onExportTxt = { viewModel.exportToTxt() },
                onExportCsv = { viewModel.exportToCsv() },
                content = listOfNotNull(
                    if (uiState.fullName.isNotBlank()) "Nombre: ${uiState.fullName}" else null,
                    if (uiState.organization.isNotBlank()) "Organización: ${uiState.organization}" else null,
                    if (uiState.address.isNotBlank()) "Dirección: ${uiState.address}" else null,
                    if (uiState.phone.isNotBlank()) "Teléfono: ${uiState.phone}" else null,
                    if (uiState.email.isNotBlank()) "Email: ${uiState.email}" else null,
                    if (uiState.notes.isNotBlank()) "Notas: ${uiState.notes}" else null
                ),
                qrBackgroundColor = uiState.backgroundColor,
                icon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) }
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Mi código QR", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { viewModel.generateVCardQr() }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Comparte tu información de contacto a través del código QR",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "Solo ingresa los datos que deseas compartir. Cuando termines, haz clic en \u2713. " +
                    "La próxima vez que abras Mi código QR, se mostrará tu código QR de contacto.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(24.dp))

                ProfileTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChanged,
                    label = "Nombre completo"
                )
                ProfileTextField(
                    value = uiState.organization,
                    onValueChange = viewModel::onOrganizationChanged,
                    label = "Organización"
                )
                ProfileTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChanged,
                    label = "Dirección"
                )
                ProfileTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChanged,
                    label = "Número de teléfono"
                )
                ProfileTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Dirección de correo electrónico"
                )
                ProfileTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChanged,
                    label = "Notas",
                    singleLine = false,
                    modifier = Modifier.height(120.dp)
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

                Spacer(Modifier.height(40.dp))

                // Banner Ad
                com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar título") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Nuevo título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(newTitle)
                    showRenameDialog = false
                }) {
                    Text("OK")
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

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}
