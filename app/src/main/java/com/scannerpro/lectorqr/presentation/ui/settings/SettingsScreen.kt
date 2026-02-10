package com.scannerpro.lectorqr.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val primaryColorLong by viewModel.primaryColor.collectAsState()
    val isBeepEnabled by viewModel.isBeepEnabled.collectAsState()
    val isVibrateEnabled by viewModel.isVibrateEnabled.collectAsState()
    val isCopyToClipboardEnabled by viewModel.isCopyToClipboardEnabled.collectAsState()
    val isUrlInfoEnabled by viewModel.isUrlInfoEnabled.collectAsState()
    val isBatchScanEnabled by viewModel.isBatchScanEnabled.collectAsState()
    val isAutofocusEnabled by viewModel.isAutofocusEnabled.collectAsState()
    val isTapToFocusEnabled by viewModel.isTapToFocusEnabled.collectAsState()
    val isKeepDuplicatesEnabled by viewModel.isKeepDuplicatesEnabled.collectAsState()
    val isAppBrowserEnabled by viewModel.isAppBrowserEnabled.collectAsState()
    val isAddToHistoryEnabled by viewModel.isAddToHistoryEnabled.collectAsState()
    val isOpenUrlAutomaticallyEnabled by viewModel.isOpenUrlAutomaticallyEnabled.collectAsState()
    val cameraSelection by viewModel.cameraSelection.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isManualPremium by viewModel.isManualPremium.collectAsState()
    var showSearchEngineDialog by remember { mutableStateOf(false) }

    val colors = listOf(
        0xFF2196F3, 0xFFF44336, 0xFFFF5722, 0xFFFFC107, 0xFF4CAF50, 0xFF00C853,
        0xFF03A9F4, 0xFF3F51B5, 0xFF9FA8DA, 0xFF9C27B0, 0xFFEF5350, 0xFF90CAF9
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(primaryColorLong))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
            // Color Scheme Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Esquema de colores", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.take(6).forEach { color ->
                                ColorCircle(
                                    color = Color(color),
                                    isSelected = primaryColorLong == color,
                                    onClick = { viewModel.setPrimaryColor(color) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.drop(6).forEach { color ->
                                ColorCircle(
                                    color = Color(color),
                                    isSelected = primaryColorLong == color,
                                    onClick = { viewModel.setPrimaryColor(color) }
                                )
                            }
                        }
                    }
                }
            }

            // Theme Item
            item {
                SettingsTextItem(
                    title = "Tema",
                    subtitle = when(themeMode) {
                        1 -> "Claro"
                        2 -> "Oscuro"
                        else -> "Predeterminado del sistema"
                    },
                    onClick = {
                        // Cycle theme for now
                        viewModel.setThemeMode((themeMode + 1) % 3)
                    }
                )
            }

            // Toggles
            item { SettingsToggleItem("Bip", isBeepEnabled) { viewModel.setBeepEnabled(it) } }
            item { SettingsToggleItem("Vibrar", isVibrateEnabled) { viewModel.setVibrateEnabled(it) } }
            item { SettingsToggleItem("Copiar al portapapeles", isCopyToClipboardEnabled) { viewModel.setCopyToClipboardEnabled(it) } }
            
            item { 
                SettingsToggleItem(
                    title = "Información de URL", 
                    subtitle = "Intenta recuperar más información sobre las URL",
                    checked = isUrlInfoEnabled
                ) { viewModel.setUrlInfoEnabled(it) } 
            }
            
            item {
                SettingsToggleItem(
                    title = "Modo de escaneo por lotes",
                    subtitle = "Añade una opción de escaneo por lotes a pantalla de escaneo",
                    checked = isBatchScanEnabled
                ) { viewModel.setBatchScanEnabled(it) }
            }

            item { SettingsToggleItem("Utilizar el enfoque automático", isAutofocusEnabled) { viewModel.setAutofocusEnabled(it) } }
            
            item {
                SettingsToggleItem(
                    title = "Toque para enfocar",
                    subtitle = "Disponible únicamente con enfoque automático encendido",
                    checked = isTapToFocusEnabled,
                    enabled = isAutofocusEnabled
                ) { viewModel.setTapToFocusEnabled(it) }
            }

            item { SettingsToggleItem("Conservar duplicados", isKeepDuplicatesEnabled) { viewModel.setKeepDuplicatesEnabled(it) } }
            
            item {
                SettingsTextItem(
                    title = "Acción personalizada",
                    subtitle = "Añade una opción para vincular a tu URL"
                ) { /* Open Custom Action Dialog */ }
            }

            item { SettingsToggleItem("Usar el navegador de la aplicación", isAppBrowserEnabled) { viewModel.setAppBrowserEnabled(it) } }
            item { SettingsToggleItem("Añadir escaneos al historial", isAddToHistoryEnabled) { viewModel.setAddToHistoryEnabled(it) } }
            
            item {
                SettingsToggleItem(
                    title = "Abrir URL automáticamente",
                    subtitle = "Abre sitios web automáticamente después de escanear QR con URL",
                    checked = isOpenUrlAutomaticallyEnabled
                ) { viewModel.setOpenUrlAutomaticallyEnabled(it) }
            }

            item {
                SettingsTextItem(
                    title = "Cámara",
                    subtitle = if (cameraSelection == 0) "Cámara 1 - recomendado" else "Cámara $cameraSelection"
                ) { viewModel.setCameraSelection((cameraSelection + 1) % 2) }
            }

            item {
                SettingsTextItem(
                    title = "Buscador",
                    subtitle = searchEngine
                ) { showSearchEngineDialog = true }
            }


            item {
                SettingsTextItem(
                    title = "Buscar país",
                    subtitle = null
                ) { /* Open Country Search */ }
            }

            item {
                SettingsTextItem(
                    title = if (isPremium) "Lector QR Pro Premium (Activo)" else "Lector QR Pro Premium",
                    subtitle = if (isPremium) "Gracias por tu apoyo" else "Quitar anuncios y desbloquear funciones"
                ) { onBack(); onMenuClick() }
            }

            // Developer Options Section
            item {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                Text(
                    "Opciones de Desarrollador",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            item {
                SettingsToggleItem(
                    title = "Activar Premium (Prueba)",
                    subtitle = "Fuerza el estado Premium de forma persistente",
                    checked = isManualPremium
                ) { viewModel.setManualPremium(it) }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Banner Ad at the bottom
        com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
            modifier = Modifier.fillMaxWidth()
        )
    }
        if (showSearchEngineDialog) {
            AlertDialog(
                onDismissRequest = { showSearchEngineDialog = false },
                title = { Text("Seleccionar Buscador") },
                text = {
                    Column {
                        listOf("Google", "Bing", "Yahoo", "DuckDuckGo", "Yandex").forEach { engine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setSearchEngine(engine)
                                        showSearchEngineDialog = false
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = searchEngine == engine,
                                    onClick = { 
                                        viewModel.setSearchEngine(engine)
                                        showSearchEngineDialog = false
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(engine, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = { },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SettingsTextItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
