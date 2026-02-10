package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun DrawerContent(
    isPremium: Boolean = false,
    onItemClick: (DrawerItem) -> Unit
) {
    val items = listOfNotNull(
        DrawerItem("Escanear", Icons.Default.QrCodeScanner, "scanner"),
        DrawerItem("Escanear imagen", Icons.Default.Image, "scan_image"),
        DrawerItem("Favoritos", Icons.Default.Star, "favorites"),
        DrawerItem("Historial", Icons.Default.History, "history"),
        DrawerItem("Mi código QR", Icons.Default.AccountBox, "my_qr"),
        DrawerItem("Crear código QR", Icons.Default.Create, "create_qr"),
        DrawerItem("Configuración", Icons.Default.Settings, "settings"),
        DrawerItem("Compartir", Icons.Default.Share, "share"),
        DrawerItem("Nuestras Apps", Icons.Default.AutoAwesome, "our_apps"),
        if (!isPremium) DrawerItem("Versión Premium", Icons.Default.WorkspacePremium, "remove_ads") else null
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lector QR Pro", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))
        
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.title) },
                selected = false,
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
