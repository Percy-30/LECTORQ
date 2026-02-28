package com.scannerpro.lectorqr.presentation.ui.create.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StandardResultView(
    paddingValues: PaddingValues,
    title: String,
    qrBitmap: Bitmap,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEditName: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onExportTxt: (() -> Unit)? = null,
    onExportCsv: (() -> Unit)? = null,
    content: List<String>,
    qrBackgroundColor: Int = android.graphics.Color.WHITE,
    icon: @Composable () -> Unit = {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
    }
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(Modifier.width(16.dp))
                Text(
                    title, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditName) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        Icons.Default.Star, 
                        "Favorito", 
                        tint = if (isFavorite) Color.Yellow else MaterialTheme.colorScheme.onSurface, 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // QR Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(280.dp)
                        .background(Color(qrBackgroundColor), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }

            // Action Buttons (Save/Share/TXT/CSV)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultActionButton(
                    icon = Icons.Default.Save,
                    label = "Guardar",
                    onClick = onSave
                )
                ResultActionButton(
                    icon = Icons.Default.Share,
                    label = "Compartir",
                    onClick = onShare
                )
                if (onExportTxt != null) {
                    ResultActionButton(
                        icon = Icons.Default.Description,
                        label = "TXT",
                        onClick = onExportTxt
                    )
                }
                if (onExportCsv != null) {
                    ResultActionButton(
                        icon = Icons.Default.TableChart,
                        label = "CSV",
                        onClick = onExportCsv
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Content details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                content.forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            line, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontSize = 18.sp, 
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Banner Ad
        com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
