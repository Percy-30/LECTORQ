package com.scannerpro.lectorqr.presentation.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scannerpro.lectorqr.R

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
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val isManualPremium by viewModel.isManualPremium.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val currentAppIcon by viewModel.currentAppIcon.collectAsState()
    var showIconDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val colors = listOf(
        0xFF2196F3, 0xFFF44336, 0xFFFF5722, 0xFFFFC107, 0xFF4CAF50, 0xFF00C853,
        0xFF03A9F4, 0xFF3F51B5, 0xFF9FA8DA, 0xFF9C27B0, 0xFFEF5350, 0xFF90CAF9
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_settings), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = "Sms", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(primaryColorLong))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_color_scheme), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
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

            item {
                val languages = listOf(
                    "system" to R.string.lang_system_default,
                    "es" to R.string.lang_es,
                    "en" to R.string.lang_en,
                    "fr" to R.string.lang_fr,
                    "de" to R.string.lang_de,
                    "it" to R.string.lang_it,
                    "pt" to R.string.lang_pt,
                    "ja" to R.string.lang_ja,
                    "zh" to R.string.lang_zh,
                    "ko" to R.string.lang_ko,
                    "ru" to R.string.lang_ru
                )
                
                val currentLanguageName = stringResource(languages.find { it.first == selectedLanguage }?.second ?: R.string.lang_system_default)
                
                SettingsTextItem(
                    title = stringResource(R.string.dialog_select_language),
                    subtitle = currentLanguageName,
                    onClick = { showLanguageDialog = true }
                )

                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = { 
                            Column {
                                Text(
                                    stringResource(R.string.dialog_select_language),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.select_your_region),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(languages.size) { index ->
                                    val (code, nameRes) = languages[index]
                                    val isSelected = selectedLanguage == code
                                    
                                    val flagEmoji = when(code) {
                                        "es" -> "🇲🇽 / 🇪🇸"
                                        "en" -> "🇺🇸 / 🇬🇧"
                                        "fr" -> "🇫🇷"
                                        "de" -> "🇩🇪"
                                        "it" -> "🇮🇹"
                                        "pt" -> "🇧🇷 / 🇵🇹"
                                        "ja" -> "🇯🇵"
                                        "zh" -> "🇨🇳"
                                        "ko" -> "🇰🇷"
                                        "ru" -> "🇷🇺"
                                        else -> "🌐"
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setSelectedLanguage(code)
                                                showLanguageDialog = false
                                            },
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(flagEmoji, fontSize = 20.sp)
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                stringResource(nameRes),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
            }

            item {
                SettingsTextItem(
                    title = stringResource(R.string.settings_theme),
                    subtitle = when(themeMode) {
                        1 -> stringResource(R.string.theme_light)
                        2 -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    onClick = {
                        viewModel.setThemeMode((themeMode + 1) % 3)
                    }
                )
            }

            item { SettingsToggleItem(stringResource(R.string.settings_beep), isBeepEnabled) { viewModel.setBeepEnabled(it) } }
            item { SettingsToggleItem(stringResource(R.string.settings_vibrate), isVibrateEnabled) { viewModel.setVibrateEnabled(it) } }
            item { SettingsToggleItem(stringResource(R.string.settings_copy_clipboard), isCopyToClipboardEnabled) { viewModel.setCopyToClipboardEnabled(it) } }
            
            item { 
                SettingsToggleItem(
                    title = stringResource(R.string.settings_url_info), 
                    subtitle = stringResource(R.string.settings_url_info_summary),
                    checked = isUrlInfoEnabled
                ) { viewModel.setUrlInfoEnabled(it) } 
            }
            
            item {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_batch_scan),
                    subtitle = stringResource(R.string.settings_batch_scan_summary),
                    checked = isBatchScanEnabled
                ) { viewModel.setBatchScanEnabled(it) }
            }

            item { SettingsToggleItem(stringResource(R.string.settings_autofocus), isAutofocusEnabled) { viewModel.setAutofocusEnabled(it) } }
            
            item {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_tap_to_focus),
                    subtitle = stringResource(R.string.settings_tap_to_focus_summary),
                    checked = isTapToFocusEnabled,
                    enabled = isAutofocusEnabled
                ) { viewModel.setTapToFocusEnabled(it) }
            }

            item { SettingsToggleItem(stringResource(R.string.settings_keep_duplicates), isKeepDuplicatesEnabled) { viewModel.setKeepDuplicatesEnabled(it) } }
            
            item {
                SettingsTextItem(
                    title = stringResource(R.string.settings_custom_action),
                    subtitle = stringResource(R.string.settings_custom_action_summary)
                ) { /* Open Custom Action Dialog */ }
            }

            item { SettingsToggleItem(stringResource(R.string.settings_app_browser), isAppBrowserEnabled) { viewModel.setAppBrowserEnabled(it) } }
            item { SettingsToggleItem(stringResource(R.string.settings_add_history), isAddToHistoryEnabled) { viewModel.setAddToHistoryEnabled(it) } }
            
            item {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_open_url_auto),
                    subtitle = stringResource(R.string.settings_open_url_auto_summary),
                    checked = isOpenUrlAutomaticallyEnabled
                ) { viewModel.setOpenUrlAutomaticallyEnabled(it) }
            }

            item {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_biometric_lock),
                    subtitle = stringResource(R.string.biometric_prompt_subtitle),
                    checked = isBiometricEnabled,
                    isPremium = isPremium
                ) { 
                    if (isPremium) viewModel.setBiometricEnabled(it)
                }
            }

            item {
                SettingsTextItem(
                    title = stringResource(R.string.settings_app_icon),
                    subtitle = currentAppIcon,
                    isPremium = isPremium
                ) { 
                    if (isPremium) showIconDialog = true
                }
            }

            item {
                SettingsTextItem(
                    title = stringResource(R.string.settings_camera),
                    subtitle = if (cameraSelection == 0) stringResource(R.string.settings_camera_recommended) else stringResource(R.string.settings_camera_n, cameraSelection)
                ) { viewModel.setCameraSelection((cameraSelection + 1) % 2) }
            }

            item {
                SettingsTextItem(
                    title = stringResource(R.string.settings_search_engine),
                    subtitle = searchEngine
                ) { showSearchEngineDialog = true }
            }


            item {
                SettingsTextItem(
                    title = if (isPremium) stringResource(R.string.settings_premium_active) else stringResource(R.string.settings_premium),
                    subtitle = if (isPremium) stringResource(R.string.settings_thanks_support) else stringResource(R.string.settings_premium_summary)
                ) { onBack(); onMenuClick() }
            }


            item { Spacer(Modifier.height(80.dp)) }
        }

        com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
            modifier = Modifier.fillMaxWidth()
        )
    }
                if (showSearchEngineDialog) {
                    AlertDialog(
                        onDismissRequest = { showSearchEngineDialog = false },
                        title = { 
                            Text(
                                stringResource(R.string.dialog_select_search_engine),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(5) { index ->
                                    val engines = listOf("Google", "Bing", "Yahoo", "DuckDuckGo", "Yandex")
                                    val engine = engines[index]
                                    val isSelected = searchEngine == engine
                                    
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setSearchEngine(engine)
                                                showSearchEngineDialog = false
                                            },
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                engine,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
                if (showIconDialog) {
                    AlertDialog(
                        onDismissRequest = { showIconDialog = false },
                        title = { Text(stringResource(R.string.settings_app_icon)) },
                        text = {
                            val icons = com.scannerpro.lectorqr.util.AppIcon.values()
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(icons.size) { index ->
                                    val icon = icons[index]
                                    val isSelected = currentAppIcon == icon.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setCurrentAppIcon(icon)
                                                showIconDialog = false
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(icon.name, modifier = Modifier.weight(1f))
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showIconDialog = false }) {
                                Text(stringResource(R.string.action_ok))
                            }
                        }
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
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    isPremium: Boolean = true,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, 
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
    isPremium: Boolean = true,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
