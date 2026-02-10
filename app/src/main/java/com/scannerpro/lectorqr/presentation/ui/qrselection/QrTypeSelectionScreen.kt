package com.scannerpro.lectorqr.presentation.ui.qrselection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerpro.lectorqr.R

data class QrType(
    val id: String,
    val title: String,
    val icon: Any,
    val isSpecial: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTypeSelectionScreen(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onTypeSelected: (String) -> Unit,
    viewModel: QrTypeSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val qrTypes = listOf(
        QrType("my_qr", "Mi código QR", Icons.Default.Person, isSpecial = true),
        QrType("url", "URL", Icons.Default.Link),
        QrType("text", "Texto", Icons.Default.TextFields),
        QrType("contact", "Contacto", Icons.Default.Person),
        QrType("email", "Dirección de correo electrónico", Icons.Default.Email),
        QrType("sms", "Dirección SMS", Icons.Default.Message),
        QrType("wifi", "Wi-Fi", Icons.Default.Wifi),
        QrType("phone", "Número de teléfono", Icons.Default.Phone),
        QrType("location", "Coordenadas geográficas", Icons.Default.LocationOn),
        QrType("calendar", "Calendario", Icons.Default.Event)
    )

    val socialTypes = listOf(
        QrType("whatsapp", "WhatsApp", R.drawable.ic_whatsapp),
        QrType("instagram", "Instagram", R.drawable.ic_instagram),
        QrType("facebook", "Facebook", R.drawable.ic_facebook),
        QrType("youtube", "YouTube", R.drawable.ic_youtube),
        QrType("twitter", "Twitter (X)", R.drawable.ic_twitter_x),
        QrType("linkedin", "LinkedIn", R.drawable.ic_linkedin),
        QrType("tiktok", "TikTok", R.drawable.ic_tiktok)
    )

    val barcodeTypes = listOf(
        QrType("ean8", "EAN_8", Icons.Default.ViewWeek),
        QrType("ean13", "EAN_13", Icons.Default.ViewWeek),
        QrType("upce", "UPC_E", Icons.Default.ViewWeek),
        QrType("upca", "UPC_A", Icons.Default.ViewWeek),
        QrType("code39", "CODE_39", Icons.Default.ViewWeek),
        QrType("code93", "CODE_93", Icons.Default.ViewWeek),
        QrType("code128", "CODE_128", Icons.Default.ViewWeek),
        QrType("itf", "ITF", Icons.Default.ViewWeek),
        QrType("pdf417", "PDF_417", Icons.Default.ViewWeek),
        QrType("codabar", "CODABAR", Icons.Default.ViewWeek),
        QrType("datamatrix", "DATA_MATRIX", Icons.Default.ViewWeek),
        QrType("aztec", "AZTEC", Icons.Default.ViewWeek)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear código QR", color = MaterialTheme.colorScheme.onPrimary) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // My QR Code - Special Item
                item {
                    QrTypeItem(
                        qrType = qrTypes[0],
                        onClick = { onTypeSelected(qrTypes[0].id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }

                // Header General Types
                item {
                    Text(
                        "Tipo de código QR",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                items(qrTypes.drop(1)) { type ->
                    QrTypeItem(
                        qrType = type,
                        onClick = { onTypeSelected(type.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }

                // Header Social
                item {
                    Text(
                        "Social",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                items(socialTypes) { type ->
                    QrTypeItem(
                        qrType = type,
                        onClick = { onTypeSelected(type.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }

                // Header Barcode
                item {
                    Text(
                        "Código de barras",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                items(barcodeTypes) { type ->
                    QrTypeItem(
                        qrType = type,
                        onClick = { onTypeSelected(type.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }

            // Banner Ad at the bottom
            com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QrTypeItem(
    qrType: QrType,
    onClick: (QrType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(qrType) }
            .background(if (qrType.isSpecial) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter = when (val icon = qrType.icon) {
            is androidx.compose.ui.graphics.vector.ImageVector -> rememberVectorPainter(icon)
            is Int -> painterResource(id = icon)
            else -> null
        }

        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = if (qrType.icon is Int) Color.Unspecified else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = qrType.title,
            color = if (qrType.isSpecial) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = if (qrType.isSpecial) FontWeight.Bold else FontWeight.Normal
        )
    }
    if (!qrType.isSpecial) {
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}
