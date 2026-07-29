package com.scannerpro.lectorqr.presentation.ui.create.dynamic

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.scannerpro.lectorqr.R
import com.scannerpro.lectorqr.presentation.ui.create.url.CreateUrlViewModel
import com.scannerpro.lectorqr.presentation.ui.create.components.StandardResultView
import com.scannerpro.lectorqr.util.DynamicQrUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDynamicQrScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: CreateUrlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputUrl by remember { mutableStateOf("") }
    val isPremium = com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(!isPremium) }

    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPremiumDialog = false
                onBack() 
            },
            title = { Text("Función Premium") },
            text = { Text("Los Códigos QR Dinámicos (con enlaces actualizables y cortos) son una característica exclusiva de la versión Premium. Adquiere la versión Premium para desbloquearla.") },
            confirmButton = {
                TextButton(onClick = { 
                    showPremiumDialog = false
                    onBack() 
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.type_dynamic_qr), color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.nav_menu), tint = MaterialTheme.colorScheme.onPrimary)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dynamic_qr_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text(stringResource(R.string.type_url)) },
                placeholder = { Text("https://www.ejemplo.com") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                isError = inputUrl.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(inputUrl).matches()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (!isPremium) {
                        showPremiumDialog = true
                        return@Button
                    }
                    var finalUrl = inputUrl.trim()
                    if (finalUrl.isNotEmpty()) {
                        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                            finalUrl = "https://$finalUrl"
                        }
                        
                        coroutineScope.launch {
                            isLoading = true
                            val dynamicUrl = DynamicQrUtils.createDynamicUrl(finalUrl)
                            isLoading = false
                            if (dynamicUrl != null) {
                                viewModel.onUrlChanged(dynamicUrl)
                                viewModel.generateQr()
                            } else {
                                Toast.makeText(context, "Error al generar enlace dinámico. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputUrl.isNotBlank() && !isLoading && isPremium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Generar código QR")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.showResult && uiState.qrBitmap != null) {
            StandardResultView(
                paddingValues = paddingValues,
                title = uiState.title,
                qrBitmap = uiState.qrBitmap!!,
                onSave = { viewModel.saveToGallery() },
                onShare = { viewModel.shareQr() },
                onEditName = { /* No-op for now */ },
                onFavoriteClick = { viewModel.toggleFavorite() },
                isFavorite = uiState.isFavorite,
                onExportTxt = { viewModel.exportToTxt() },
                onExportCsv = { viewModel.exportToCsv() },
                qrBackgroundColor = uiState.backgroundColor,
                icon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) },
                content = listOf("URL Dinámica: ${uiState.url}")
            )
        }
    }
}
