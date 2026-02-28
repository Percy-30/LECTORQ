package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scannerpro.lectorqr.R

data class DrawerItem(
    val titleRes: Int,
    val icon: ImageVector,
    val route: String
)

@Composable
fun DrawerContent(
    isPremium: Boolean = false,
    onItemClick: (DrawerItem) -> Unit
) {
    val items = listOfNotNull(
        DrawerItem(R.string.drawer_scan, Icons.Default.QrCodeScanner, "scanner"),
        DrawerItem(R.string.drawer_scan_image, Icons.Default.Image, "scan_image"),
        DrawerItem(R.string.drawer_favorites, Icons.Default.Star, "favorites"),
        DrawerItem(R.string.drawer_history, Icons.Default.History, "history"),
        DrawerItem(R.string.drawer_my_qr, Icons.Default.AccountBox, "my_qr"),
        DrawerItem(R.string.drawer_create_qr, Icons.Default.Create, "create_qr"),
        DrawerItem(R.string.drawer_settings, Icons.Default.Settings, "settings"),
        DrawerItem(R.string.drawer_share, Icons.Default.Share, "share"),
        DrawerItem(R.string.drawer_our_apps, Icons.Default.AutoAwesome, "our_apps"),
        if (!isPremium) DrawerItem(R.string.drawer_premium, Icons.Default.WorkspacePremium, "remove_ads") else null
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.drawer_title), 
            style = MaterialTheme.typography.headlineSmall, 
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        items.forEach { item ->
            NavigationDrawerItem(
                label = { Text(stringResource(item.titleRes)) },
                selected = false,
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
