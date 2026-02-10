package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTopBar(
    isFlashEnabled: Boolean,
    isBatchScanEnabled: Boolean = false,
    isBatchModeActive: Boolean = false,
    onMenuClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit,
    onBatchModeToggle: () -> Unit = {}
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            Row {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu, 
                        contentDescription = "Menu", 
                        tint = if (isBatchModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface 
                    )
                }
                IconButton(onClick = onGalleryClick) {
                    Icon(
                        Icons.Default.Image, 
                        contentDescription = "Gallery", 
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (isBatchScanEnabled) {
                IconButton(onClick = onBatchModeToggle) {
                    Icon(
                        Icons.Default.GridView, 
                        contentDescription = "Batch Mode", 
                        tint = if (isBatchModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onFlashToggle) {
                Icon(
                    if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = if (isFlashEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onCameraFlip) {
                Icon(
                    Icons.Default.FlipCameraAndroid, 
                    contentDescription = "Flip Camera", 
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
