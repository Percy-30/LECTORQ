package com.scannerpro.lectorqr.presentation.ui.scanner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.presentation.ui.components.BannerAdView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: Long,
    onBack: () -> Unit,
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
            onRenameInputChange = { viewModel.updateRenameInput(it) },
            onDelete = { 
                viewModel.deleteScan()
                onBack()
            },
            onExportTxt = { viewModel.exportAsTxt() },
            onExportCsv = { viewModel.exportAsCsv() },
            onGetSearchUrl = { viewModel.getSearchUrl(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultContent(
    uiState: ScanResultUiState,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenRename: () -> Unit,
    onCloseRename: () -> Unit,
    onSaveName: () -> Unit,
    onRenameInputChange: (String) -> Unit,
    onDelete: () -> Unit,
    onExportTxt: () -> Unit,
    onExportCsv: () -> Unit,
    onGetSearchUrl: (String) -> String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val displayResult = uiState.result ?: return

    val sdf = SimpleDateFormat("d MMM. yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(displayResult.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { 
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar TXT", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { 
                                    showMenu = false
                                    onExportTxt()
                                },
                                leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar CSV", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { 
                                    showMenu = false
                                    onExportCsv()
                                },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Renombrar", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { 
                                    showMenu = false
                                    onOpenRename()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
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
                        imageVector = Icons.Default.TextFields, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.customName.isNotEmpty()) uiState.customName else "Texto", 
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
                        contentDescription = "Rename", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { clipboardManager.setText(AnnotatedString(displayResult.rawValue ?: "")) }) {
                    Icon(
                        Icons.Default.ContentCopy, 
                        contentDescription = "Copy", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (displayResult.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (displayResult.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (uiState.isRenameDialogOpen) {
                AlertDialog(
                    onDismissRequest = onCloseRename,
                    title = { Text("Editar nombre", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        OutlinedTextField(
                            value = uiState.renameInput,
                            onValueChange = onRenameInputChange,
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
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onCloseRename) {
                            Text("CANCELAR", color = MaterialTheme.colorScheme.primary)
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
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = displayResult.displayValue ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultActionItem(Icons.Default.Search, "Búsqueda\nWeb") {
                    try {
                        val searchUrl = onGetSearchUrl(displayResult.rawValue ?: "")
                        if (uiState.isAppBrowserEnabled) {
                            val builder = androidx.browser.customtabs.CustomTabsIntent.Builder()
                            val customTabsIntent = builder.build()
                            customTabsIntent.launchUrl(context, Uri.parse(searchUrl))
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) { }
                }
                ResultActionItem(Icons.Default.Share, "Compartir") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, displayResult.rawValue)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir"))
                }
                ResultActionItem(Icons.Default.ContentCopy, "Copiar") {
                    clipboardManager.setText(AnnotatedString(displayResult.rawValue ?: ""))
                }
            }

            // QR Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White) // QR needs white background
                        .padding(8.dp)
                ) {
                    if (displayResult.imagePath != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(displayResult.imagePath)
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = Color.Black)
                        }
                    } else {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = Color.Black)
                    }

                    // Blue Corners Overlay
                    val cornerColor = MaterialTheme.colorScheme.primary
                    val strokeWidth = 3.dp
                    val cornerSize = 20.dp

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        if (width > 0 && height > 0) {
                            val strokePx = strokeWidth.toPx()
                            val cornerPx = cornerSize.toPx().coerceAtMost(width / 2).coerceAtMost(height / 2)

                            // Top Left
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            // Top Right
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - cornerPx, 0f), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - strokePx, 0f), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            // Bottom Left
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(0f, height - strokePx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(0f, height - cornerPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))

                            // Bottom Right
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - cornerPx, height - strokePx), size = androidx.compose.ui.geometry.Size(cornerPx, strokePx))
                            drawRect(color = cornerColor, topLeft = androidx.compose.ui.geometry.Offset(width - strokePx, height - cornerPx), size = androidx.compose.ui.geometry.Size(strokePx, cornerPx))
                        }
                    }
                }
            }

            // Banner Ad at the very bottom
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ResultActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 12.sp, 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
