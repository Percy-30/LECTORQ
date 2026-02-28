package com.scannerpro.lectorqr.presentation.ui.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorPickerSection(
    title: String,
    selectedColor: Int,
    isPremium: Boolean,
    onColorSelected: (Int) -> Unit
) {
    val colors = listOf(
        android.graphics.Color.BLACK,
        android.graphics.Color.WHITE,
        android.graphics.Color.parseColor("#FFD700"), // Gold
        android.graphics.Color.parseColor("#E91E63"), // Pink
        android.graphics.Color.parseColor("#2196F3"), // Blue
        android.graphics.Color.parseColor("#4CAF50"), // Green
        android.graphics.Color.parseColor("#FF5722"), // Orange
        android.graphics.Color.parseColor("#9C27B0")  // Purple
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (!isPremium) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { colorInt ->
                val color = Color(colorInt)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color, RoundedCornerShape(8.dp))
                        .border(
                            width = if (selectedColor == colorInt) 2.dp else 1.dp,
                            color = if (selectedColor == colorInt) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = isPremium) { onColorSelected(colorInt) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == colorInt) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp), 
                            tint = if (colorInt == android.graphics.Color.WHITE) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}
