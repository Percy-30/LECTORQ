package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTopBar(
    isFlashEnabled: Boolean,
    onMenuClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            Row {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
                IconButton(onClick = onGalleryClick) {
                    Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                }
            }
        },
        actions = {
            IconButton(onClick = onFlashToggle) {
                Icon(
                    if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
            IconButton(onClick = onCameraFlip) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
