package com.scannerpro.lectorqr.presentation.ui.scanner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.scannerpro.lectorqr.R
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.presentation.ui.components.BannerAdView
import com.scannerpro.lectorqr.util.BarcodeTypeUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: Long,
    onBack: () -> Unit,
    onEdit: (String) -> Unit = {},
    viewModel: ScanResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(scanId) {
        viewModel.init(scanId)
    }

    if (uiState.isLoading || uiState.result == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        ScanResultContent(
            uiState = uiState,
            onBack = onBack,
            onToggleFavorite = { viewModel.toggleFavorite() },
            onOpenRename = { viewModel.openRenameDialog() },
            onCloseRename = { viewModel.closeRenameDialog() },
            onSaveName = { viewModel.saveName() },
            onUpdateRenameInput = { viewModel.updateRenameInput(it) },
            onDelete = { 
                viewModel.deleteScan()
                onBack()
            },
            onExportTxt = { share -> viewModel.exportAsTxt(share) },
            onExportCsv = { share -> viewModel.exportAsCsv(share) },
            onGetSearchUrl = { viewModel.getSearchUrl(it) },
            onShare = { /* This callback is not directly used here, but passed down */ },
            onEdit = {
                val route = viewModel.prepareEditAndGetRoute()
                if (route != null) {
                    onEdit(route)
                }
            },
            onSaveQr = { viewModel.saveQrToGallery() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanResultContent(
    uiState: ScanResultUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenRename: () -> Unit,
    onCloseRename: () -> Unit,
    onSaveName: () -> Unit,
    onUpdateRenameInput: (String) -> Unit,
    onDelete: () -> Unit,
    onExportTxt: (Boolean) -> Unit,
    onExportCsv: (Boolean) -> Unit,
    onGetSearchUrl: (String) -> String,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onSaveQr: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var resultSubMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val displayResult = uiState.result ?: return

    val sdf = SimpleDateFormat("d MMM. yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(displayResult.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_scan), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options_more), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { 
                                showMenu = false
                                resultSubMenu = null
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            when (resultSubMenu) {
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
                                        text = { Text(stringResource(R.string.action_rename), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            onOpenRename()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Editar", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_txt), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { resultSubMenu = "TXT" }
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_csv), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { resultSubMenu = "CSV" }
                                    )
                                }
                                "TXT" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { resultSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            resultSubMenu = null
                                            onExportTxt(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            resultSubMenu = null
                                            onExportTxt(false)
                                        }
                                    )
                                }
                                "CSV" -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.nav_back), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { resultSubMenu = null }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            resultSubMenu = null
                                            onExportCsv(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            showMenu = false
                                            resultSubMenu = null
                                            onExportCsv(false)
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val icon = BarcodeTypeUtils.getIconForType(displayResult.type)
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = if (uiState.customName.isNotEmpty() && uiState.customName != "Texto" && uiState.customName != "Text") {
                        uiState.customName
                    } else {
                        stringResource(BarcodeTypeUtils.getTypeNameRes(displayResult.type))
                    }
                    Text(
                        text = displayName, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp
                    )
                    Text(
                        text = "$dateString, QR_CODE", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 13.sp
                    )
                }
                
                IconButton(onClick = onOpenRename) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = stringResource(R.string.action_rename), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { 
                    val items = BarcodeTypeUtils.getFormattedValueWithLabels(displayResult.type, displayResult.rawValue)
                    val formattedText = if (items.isEmpty()) displayResult.rawValue ?: "" 
                                      else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }
                    clipboardManager.setText(AnnotatedString(formattedText)) 
                }) {
                    Icon(
                        Icons.Default.ContentCopy, 
                        contentDescription = stringResource(R.string.action_copy), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (displayResult.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.action_favorite),
                        tint = if (displayResult.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (uiState.isRenameDialogOpen) {
                AlertDialog(
                    onDismissRequest = onCloseRename,
                    title = { Text(stringResource(R.string.action_rename_title), color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        OutlinedTextField(
                            value = uiState.renameInput,
                            onValueChange = onUpdateRenameInput,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = onSaveName) {
                            Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onCloseRename) {
                            Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val items = BarcodeTypeUtils.getFormattedValueWithLabels(displayResult.type, displayResult.rawValue)
                
                if (items.isNotEmpty()) {
                    FormattedDetailContent(items)
                } else {
                    Row {
                        Text(
                            text = stringResource(R.string.content_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayResult.displayValue ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Dynamic Actions
            DynamicActionsRow(displayResult, onGetSearchUrl, onSaveQr)

            // QR Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrBgColor = if (displayResult.backgroundColor != null) Color(displayResult.backgroundColor) else Color.White
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(qrBgColor)
                        .padding(16.dp)
                ) {
                    var bitmapLoaded = false
                    
                    // Prioritize dynamically generated bitmap from ViewModel (which has colors)
                    if (uiState.qrBitmap != null) {
                        bitmapLoaded = true
                        androidx.compose.foundation.Image(
                            bitmap = uiState.qrBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else if (displayResult.imagePath != null) {
                        val bitmap = try {
                            android.graphics.BitmapFactory.decodeFile(displayResult.imagePath)
                        } catch(e: Exception) {
                            android.util.Log.e("ScanResultScreen", "Error loading bitmap from path: ${displayResult.imagePath}", e)
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
                        // Fallback generic icon based on the Barcode type
                        val previewIcon = BarcodeTypeUtils.getIconForType(displayResult.type)
                        Icon(previewIcon, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = if (qrBgColor == Color.White) Color.Black else Color.White)
                    }

                    val cornerColor = Color(0xFFFFCC33) 
                    val strokeWidth = 3.dp
                    val cornerSize = 25.dp
                    val offset = (-6).dp

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        if (width > 0 && height > 0) {
                            val strokePx = strokeWidth.toPx()
                            val cornerPx = cornerSize.toPx()
                            val offsetPx = offset.toPx()

                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(offsetPx, offsetPx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(offsetPx, offsetPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - cornerPx - offsetPx, offsetPx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - strokePx - offsetPx, offsetPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(offsetPx, height - strokePx - offsetPx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(offsetPx, height - cornerPx - offsetPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - cornerPx - offsetPx, height - strokePx - offsetPx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - strokePx - offsetPx, height - cornerPx - offsetPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormattedDetailContent(items: List<Pair<Int, String>>) {
    if (items.isEmpty()) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 250.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            items.forEach { (labelRes, value) ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(labelRes),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicActionsRow(result: BarcodeResult, onGetSearchUrl: (String) -> String, onSaveQr: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val type = result.type
    val rawValue = result.rawValue ?: ""
    val items = BarcodeTypeUtils.getFormattedValueWithLabels(type, rawValue)
    val formattedText = if (items.isEmpty()) rawValue
                       else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }

    val tel = if (type == Barcode.TYPE_CONTACT_INFO) rawValue.substringAfter("TEL:", "").substringBefore("\n").trim() else rawValue
    val email = if (type == Barcode.TYPE_CONTACT_INFO) rawValue.substringAfter("EMAIL:", "").substringBefore("\n").trim() else rawValue
    val geo = if (type == Barcode.TYPE_GEO) rawValue.substringAfter("geo:", "").substringBefore("?").ifEmpty { rawValue } else ""

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        maxItemsInEachRow = 4
    ) {
        when (type) {
            Barcode.TYPE_CONTACT_INFO -> {
                ResultActionItem(Icons.Default.PersonAdd, stringResource(R.string.btn_add_contact)) {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        this.type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                        val name = rawValue.substringAfter("FN:", "").substringBefore("\n").trim()
                        putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, tel)
                        putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, email)
                    }
                    context.startActivity(intent)
                }
                if (tel.isNotEmpty()) {
                    ResultActionItem(Icons.Default.Call, stringResource(R.string.btn_dial_number)) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                        context.startActivity(intent)
                    }
                }
            }
            Barcode.TYPE_GEO -> {
                ResultActionItem(Icons.Default.Map, stringResource(R.string.btn_show_map)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$geo?q=$geo"))
                    context.startActivity(intent)
                }
            }
            Barcode.TYPE_PHONE -> {
                ResultActionItem(Icons.Default.Call, stringResource(R.string.btn_call)) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawValue"))
                    context.startActivity(intent)
                }
            }
            Barcode.TYPE_EMAIL -> {
                ResultActionItem(Icons.Default.Email, stringResource(R.string.btn_send_email)) {
                    val to = rawValue.substringAfter("MATMSG:TO:", "").substringBefore(";", "").ifEmpty {
                        rawValue.substringAfter("mailto:", "").substringBefore("?")
                    }
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to"))
                    context.startActivity(intent)
                }
            }
            Barcode.TYPE_SMS -> {
                val phone = rawValue.substringAfter("smsto:", "").substringBefore(":").ifEmpty {
                    rawValue.substringAfter("SMSTO:", "").substringBefore(":")
                }
                if (phone.isNotEmpty()) {
                    ResultActionItem(Icons.Default.Sms, stringResource(R.string.btn_send_sms)) {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                        context.startActivity(intent)
                    }
                }
            }
            Barcode.TYPE_URL -> {
                ResultActionItem(Icons.Default.Language, stringResource(R.string.btn_open_url)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawValue))
                    context.startActivity(intent)
                }
            }
            Barcode.TYPE_WIFI -> {
                val pass = rawValue.substringAfter("P:", "").substringBefore(";", "")
                if (pass.isNotEmpty()) {
                    ResultActionItem(Icons.Default.VpnKey, stringResource(R.string.btn_copy_password)) {
                        clipboardManager.setText(AnnotatedString(pass))
                    }
                }
            }
        }

        ResultActionItem(Icons.Default.Search, stringResource(R.string.btn_web_search)) {
            val searchUrl = onGetSearchUrl(rawValue)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            context.startActivity(intent)
        }
        
        ResultActionItem(Icons.Default.Save, stringResource(R.string.action_save)) {
            onSaveQr()
        }
        
        ResultActionItem(Icons.Default.Share, stringResource(R.string.action_share)) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedText)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        }

        ResultActionItem(Icons.Default.ContentCopy, stringResource(R.string.action_copy)) {
            clipboardManager.setText(AnnotatedString(formattedText))
        }
    }
}

@Composable
fun ResultActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 11.sp, 
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}
