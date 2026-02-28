package com.scannerpro.lectorqr.presentation.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.presentation.ui.history.HistoryItem
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import com.scannerpro.lectorqr.util.FileUtils
import androidx.compose.material.icons.filled.*
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.outlined.FilterList
import com.scannerpro.lectorqr.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onResultSelected: (BarcodeResult) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAuthenticated by remember { mutableStateOf(!uiState.isBiometricEnabled) }
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isBiometricEnabled) {
        if (uiState.isBiometricEnabled && !isAuthenticated) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            if (activity != null) {
                val biometricHelper = com.scannerpro.lectorqr.util.BiometricHelper(activity)
                if (biometricHelper.canAuthenticate()) {
                    biometricHelper.authenticate(
                        onSuccess = { isAuthenticated = true },
                        onError = { onBack() }
                    )
                } else {
                    isAuthenticated = true
                }
            } else {
                isAuthenticated = true
            }
        }
    }

    if (!isAuthenticated && uiState.isBiometricEnabled) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameScanId by remember { mutableLongStateOf(-1L) }
    var renameInput by remember { mutableStateOf("") }

    var showFilterMenu by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var actionsSubMenu by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_favorites), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = if (uiState.selectedFilter != null) Icons.Default.FilterAlt else Icons.Outlined.FilterList, 
                                contentDescription = stringResource(R.string.options_more), 
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            val filters = listOf(
                                null to R.string.all_filters,
                                Barcode.TYPE_WIFI to R.string.type_wifi,
                                Barcode.TYPE_URL to R.string.type_url,
                                Barcode.TYPE_TEXT to R.string.type_text,
                                Barcode.TYPE_CONTACT_INFO to R.string.type_contact,
                                Barcode.TYPE_EMAIL to R.string.type_email,
                                Barcode.TYPE_SMS to R.string.type_sms,
                                Barcode.TYPE_GEO to R.string.type_geo,
                                Barcode.TYPE_CALENDAR_EVENT to R.string.type_calendar,
                                Barcode.TYPE_PRODUCT to R.string.type_product,
                                Barcode.TYPE_ISBN to R.string.type_isbn
                            )

                            filters.forEach { (type, labelRes) ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            stringResource(labelRes), 
                                            color = if (uiState.selectedFilter == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (uiState.selectedFilter == type) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = if (type == null) Icons.Default.AllInclusive else BarcodeTypeUtils.getIconForType(type), 
                                            contentDescription = null, 
                                            tint = if (uiState.selectedFilter == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    },
                                    onClick = {
                                        showFilterMenu = false
                                        viewModel.setFilter(type)
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options_more), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { 
                                showActionsMenu = false
                                actionsSubMenu = null
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            when (actionsSubMenu) {
                                null -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            viewModel.deleteScans(uiState.scans.map { it.id })
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_txt), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { actionsSubMenu = "TXT" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_csv), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { actionsSubMenu = "CSV" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_group), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            viewModel.shareGroup(uiState.scans)
                                        }
                                    )
                                }
                                "TXT" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { actionsSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            actionsSubMenu = null
                                            viewModel.exportGroupAsTxt(uiState.scans, isShare = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            actionsSubMenu = null
                                            viewModel.exportGroupAsTxt(uiState.scans, isShare = false)
                                        }
                                    )
                                }
                                "CSV" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { actionsSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            actionsSubMenu = null
                                            viewModel.exportGroupAsCsv(uiState.scans, isShare = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showActionsMenu = false
                                            actionsSubMenu = null
                                            viewModel.exportGroupAsCsv(uiState.scans, isShare = false)
                                        }
                                    )
                                }
                            }
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
                if (uiState.scans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_favorites), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.scans) { scan ->
                            HistoryItem(
                                scan = scan, 
                                isPremium = uiState.isPremium,
                                onClick = { onResultSelected(scan) },
                                onToggleFavorite = { viewModel.toggleFavorite(scan) },
                                onDelete = { viewModel.deleteScan(scan.id) },
                                onRename = { 
                                    renameScanId = scan.id
                                    renameInput = scan.customName ?: ""
                                    showRenameDialog = true
                                },
                                onExportTxt = { isShare -> viewModel.exportIndividualAsTxt(scan, isShare) },
                                onExportCsv = { isShare -> viewModel.exportIndividualAsCsv(scan, isShare) }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }
            }
            
            com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(stringResource(R.string.action_rename)) },
                text = {
                    TextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateName(renameScanId, renameInput)
                        showRenameDialog = false
                    }) {
                        Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
