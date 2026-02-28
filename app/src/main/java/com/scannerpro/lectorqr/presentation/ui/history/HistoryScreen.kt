package com.scannerpro.lectorqr.presentation.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.fragment.app.FragmentActivity
import com.scannerpro.lectorqr.R
import com.scannerpro.lectorqr.util.BarcodeTypeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onResultSelected: (BarcodeResult) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
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
    var showMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var expandedHeaderDate by remember { mutableStateOf<String?>(null) }
    var headerSubMenu by remember { mutableStateOf<String?>(null) }
    var topMenuSubMenu by remember { mutableStateOf<String?>(null) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameScanId by remember { mutableLongStateOf(-1L) }
    var renameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_history), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList, 
                                contentDescription = stringResource(R.string.options_more), 
                                tint = if (uiState.selectedFilter != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(220.dp)
                        ) {
                            val filters = listOf(
                                null to R.string.all_filters,
                                Barcode.TYPE_URL to R.string.type_url,
                                Barcode.TYPE_TEXT to R.string.type_text,
                                Barcode.TYPE_WIFI to R.string.type_wifi,
                                Barcode.TYPE_PRODUCT to R.string.type_product,
                                Barcode.TYPE_PHONE to R.string.type_phone,
                                Barcode.TYPE_CONTACT_INFO to R.string.type_contact,
                                Barcode.TYPE_ISBN to R.string.type_isbn,
                                Barcode.TYPE_EMAIL to R.string.type_email,
                                Barcode.TYPE_SMS to R.string.type_sms,
                                Barcode.TYPE_GEO to R.string.type_geo,
                                Barcode.TYPE_CALENDAR_EVENT to R.string.type_calendar
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
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options_more), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { 
                                showMenu = false 
                                topMenuSubMenu = null
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            when (topMenuSubMenu) {
                                null -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.clearHistory()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_txt), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { topMenuSubMenu = "TXT" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_csv), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { topMenuSubMenu = "CSV" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_all), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            val allScans = uiState.groupedScans.values.flatten()
                                            viewModel.shareGroup(allScans)
                                        }
                                    )
                                }
                                "TXT" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { topMenuSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            topMenuSubMenu = null
                                            val allScans = uiState.groupedScans.values.flatten()
                                            viewModel.exportGroupAsTxt("Completo", allScans, isShare = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            topMenuSubMenu = null
                                            val allScans = uiState.groupedScans.values.flatten()
                                            viewModel.exportGroupAsTxt("Completo", allScans, isShare = false)
                                        }
                                    )
                                }
                                "CSV" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { topMenuSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            topMenuSubMenu = null
                                            val allScans = uiState.groupedScans.values.flatten()
                                            viewModel.exportGroupAsCsv("Completo", allScans, isShare = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            topMenuSubMenu = null
                                            val allScans = uiState.groupedScans.values.flatten()
                                            viewModel.exportGroupAsCsv("Completo", allScans, isShare = false)
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
                if (uiState.groupedScans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        uiState.groupedScans.forEach { (date, scans) ->
                            stickyHeader {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = date,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Box {
                                        IconButton(onClick = { 
                                            expandedHeaderDate = date
                                            headerSubMenu = null 
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.group_options),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expandedHeaderDate == date,
                                            onDismissRequest = { 
                                                expandedHeaderDate = null
                                                headerSubMenu = null
                                            },
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            when (headerSubMenu) {
                                                null -> {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            viewModel.deleteScans(scans.map { it.id })
                                                        }
                                                    )
                                                    
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_txt), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = { headerSubMenu = "TXT" }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_csv), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = { headerSubMenu = "CSV" }
                                                    )

                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.share_group), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            viewModel.shareGroup(scans)
                                                        }
                                                    )
                                                }
                                                "TXT" -> {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = { headerSubMenu = null }
                                                    )
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            headerSubMenu = null
                                                            viewModel.exportGroupAsTxt(date, scans, isShare = true)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            headerSubMenu = null
                                                            viewModel.exportGroupAsTxt(date, scans, isShare = false)
                                                        }
                                                    )
                                                }
                                                "CSV" -> {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = { headerSubMenu = null }
                                                    )
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            headerSubMenu = null
                                                            viewModel.exportGroupAsCsv(date, scans, isShare = true)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        onClick = {
                                                            expandedHeaderDate = null
                                                            headerSubMenu = null
                                                            viewModel.exportGroupAsCsv(date, scans, isShare = false)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            items(scans) { scan ->
                                HistoryItem(
                                    scan = scan, 
                                    isPremium = uiState.isPremium,
                                    onClick = { onResultSelected(scan) },
                                    onToggleFavorite = { viewModel.toggleFavorite(scan.id, scan.isFavorite) },
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

@Composable
fun HistoryItem(
    scan: BarcodeResult, 
    isPremium: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onExportTxt: (Boolean) -> Unit,
    onExportCsv: (Boolean) -> Unit
) {
    val sdf = SimpleDateFormat("d/MM/yy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(scan.timestamp))
    var showMenu by remember { mutableStateOf(false) }
    var itemSubMenu by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            var bitmapLoaded = false
            if (isPremium && scan.imagePath != null) {
                val bitmap = try {
                    android.graphics.BitmapFactory.decodeFile(scan.imagePath)
                } catch(e: Exception) {
                    android.util.Log.e("HistoryScreen", "Error loading bitmap from path: ${scan.imagePath}", e)
                    null
                }
                
                if (bitmap != null) {
                    bitmapLoaded = true
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            } 
            
            if (!bitmapLoaded) {
                val drawableRes = BarcodeTypeUtils.getDrawableForScan(scan.type, scan.customName)
                if (drawableRes != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = BarcodeTypeUtils.getIconForType(scan.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val displayName = if (scan.customName != null && scan.customName.isNotBlank()) {
                scan.customName
            } else {
                stringResource(BarcodeTypeUtils.getTypeNameRes(scan.type))
            }
            Text(
                text = displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = "$dateString, QR_CODE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            val items = BarcodeTypeUtils.getFormattedValueWithLabels(scan.type, scan.rawValue)
            val formattedText = if (items.isEmpty()) scan.rawValue ?: ""
                               else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }
            Text(
                text = formattedText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (scan.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = stringResource(R.string.action_favorite),
                tint = if (scan.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.options_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { 
                    showMenu = false
                    itemSubMenu = null
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                when (itemSubMenu) {
                    null -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_txt), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { itemSubMenu = "TXT" }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_csv), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { itemSubMenu = "CSV" }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                val items = BarcodeTypeUtils.getFormattedValueWithLabels(scan.type, scan.rawValue)
                                val formattedValue = if (items.isEmpty()) scan.rawValue ?: ""
                                                   else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, formattedValue)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.action_share)))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                val items = BarcodeTypeUtils.getFormattedValueWithLabels(scan.type, scan.rawValue)
                                val formattedValue = if (items.isEmpty()) scan.rawValue ?: ""
                                                   else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(formattedValue))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                    }
                    "TXT" -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { itemSubMenu = null }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                itemSubMenu = null
                                onExportTxt(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                itemSubMenu = null
                                onExportTxt(false)
                            }
                        )
                    }
                    "CSV" -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { itemSubMenu = null }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                itemSubMenu = null
                                onExportCsv(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                showMenu = false
                                itemSubMenu = null
                                onExportCsv(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
