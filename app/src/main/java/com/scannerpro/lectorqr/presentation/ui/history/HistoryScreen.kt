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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onResultSelected: (BarcodeResult) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    // Rename dialog state
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameScanId by remember { mutableLongStateOf(-1L) }
    var renameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Toggle filter? */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color(0xFF333333)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Eliminar todo", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("CSV", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("TXT", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2196F3))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (uiState.groupedScans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No hay historial aún", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                uiState.groupedScans.forEach { (date, scans) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = date,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(scans) { scan ->
                        HistoryItem(
                            scan = scan, 
                            onClick = { onResultSelected(scan) },
                            onToggleFavorite = { viewModel.toggleFavorite(scan.id, scan.isFavorite) },
                            onDelete = { viewModel.deleteScan(scan.id) },
                            onRename = { 
                                renameScanId = scan.id
                                renameInput = scan.customName ?: "Texto"
                                showRenameDialog = true
                            }
                        )
                        Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                    }
                }
            }
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Renombrar") },
                text = {
                    TextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color(0xFF2196F3)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateName(renameScanId, renameInput)
                        showRenameDialog = false
                    }) {
                        Text("Guardar", color = Color(0xFF2196F3))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF333333),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun HistoryItem(
    scan: BarcodeResult, 
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val sdf = SimpleDateFormat("d/MM/yy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(scan.timestamp))
    var showMenu by remember { mutableStateOf(false) }
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
            Icon(
                imageVector = Icons.Default.TextFields,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scan.customName ?: "Texto",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = "$dateString, QR_CODE",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = scan.displayValue ?: "",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (scan.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                tint = if (scan.isFavorite) Color(0xFF2196F3) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color(0xFF333333)
            ) {
                DropdownMenuItem(
                    text = { Text("Eliminar", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
                DropdownMenuItem(
                    text = { Text("TXT", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, scan.displayValue)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
                DropdownMenuItem(
                    text = { Text("CSV", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        val csv = "${scan.customName},${dateString},${scan.displayValue}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Compartir", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, scan.displayValue)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copiar", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        clipboardManager.setText(AnnotatedString(scan.displayValue ?: ""))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Renombrar", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
            }
        }
    }
}
