package com.scannerpro.lectorqr.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf

val LocalIsPremium = compositionLocalOf { false }

@Composable
fun LectorQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = Color(0xFF2196F3),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = primaryColor.copy(alpha = 0.8f),
            tertiary = primaryColor.copy(alpha = 0.6f),
            surface = Color(0xFF121212),
            background = Color(0xFF121212)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = primaryColor.copy(alpha = 0.8f),
            tertiary = primaryColor.copy(alpha = 0.6f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
