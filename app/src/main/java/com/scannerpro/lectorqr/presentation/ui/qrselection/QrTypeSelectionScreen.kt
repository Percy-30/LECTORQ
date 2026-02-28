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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerpro.lectorqr.R

data class QrType(
    val id: String,
    val titleRes: Int,
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
        QrType("my_qr", R.string.drawer_my_qr, Icons.Default.Person, isSpecial = true),
        QrType("url", R.string.type_url, Icons.Default.Link),
        QrType("text", R.string.type_text, Icons.Default.TextFields),
        QrType("contact", R.string.type_contact, Icons.Default.Person),
        QrType("email", R.string.label_email_address, Icons.Default.Email),
        QrType("sms", R.string.type_sms, Icons.Default.Message),
        QrType("wifi", R.string.type_wifi, Icons.Default.Wifi),
        QrType("phone", R.string.label_phone_number, Icons.Default.Phone),
        QrType("location", R.string.field_coordinates, Icons.Default.LocationOn),
        QrType("calendar", R.string.type_calendar, Icons.Default.Event)
    )

    val socialTypes = listOf(
        QrType("whatsapp", R.string.type_whatsapp, R.drawable.ic_whatsapp),
        QrType("instagram", R.string.type_instagram, R.drawable.ic_instagram),
        QrType("facebook", R.string.type_facebook, R.drawable.ic_facebook),
        QrType("youtube", R.string.type_youtube, R.drawable.ic_youtube),
        QrType("twitter", R.string.type_twitter, R.drawable.ic_twitter_x),
        QrType("linkedin", R.string.type_linkedin, R.drawable.ic_linkedin),
        QrType("tiktok", R.string.type_tiktok, R.drawable.ic_tiktok)
    )

    val barcodeTypes = listOf(
        QrType("ean8", R.string.type_ean8, Icons.Default.ViewWeek),
        QrType("ean13", R.string.type_ean13, Icons.Default.ViewWeek),
        QrType("upce", R.string.type_upce, Icons.Default.ViewWeek),
        QrType("upca", R.string.type_upca, Icons.Default.ViewWeek),
        QrType("code39", R.string.type_code39, Icons.Default.ViewWeek),
        QrType("code93", R.string.type_code93, Icons.Default.ViewWeek),
        QrType("code128", R.string.type_code128, Icons.Default.ViewWeek),
        QrType("itf", R.string.type_itf, Icons.Default.ViewWeek),
        QrType("pdf417", R.string.type_pdf417, Icons.Default.ViewWeek),
        QrType("codabar", R.string.type_codabar, Icons.Default.ViewWeek),
        QrType("datamatrix", R.string.type_datamatrix, Icons.Default.ViewWeek),
        QrType("aztec", R.string.type_aztec, Icons.Default.ViewWeek)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_type_selection_title), color = MaterialTheme.colorScheme.onPrimary) },
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
                        stringResource(R.string.qr_type_general_header),
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
                        stringResource(R.string.qr_type_social_header),
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
                        stringResource(R.string.qr_type_barcode_header),
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
            text = stringResource(qrType.titleRes),
            color = if (qrType.isSpecial) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = if (qrType.isSpecial) FontWeight.Bold else FontWeight.Normal
        )
    }
}
