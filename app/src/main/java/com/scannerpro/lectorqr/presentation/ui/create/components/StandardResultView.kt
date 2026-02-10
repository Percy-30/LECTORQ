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
    content: List<String>
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
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.size(28.dp)
                )
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
                        .background(Color.White, RoundedCornerShape(8.dp)) // QR needs white
                        .padding(16.dp)
                )
            }

            // Action Buttons (Save/Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            Icons.Default.Save, 
                            "Guardar", 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Text("Guardar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            Icons.Default.Share, 
                            "Compartir", 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Text("Compartir", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
