package com.scannerpro.lectorqr.presentation.ui.scanner

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@Composable
fun ScannerViewfinder() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "lineOffset"
    )

    val cornerColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = width * 0.7f
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2
        
        // Background dimming
        drawRect(
            color = Color.Black.copy(alpha = 0.5f)
        )
        
        // Clear box
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        val cornerLength = 40.dp.toPx()
        val strokeWidth = 6.dp.toPx()

        // Top Left
        drawLine(cornerColor, Offset(left - strokeWidth/2, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top Right
        drawLine(cornerColor, Offset(left + boxSize + strokeWidth/2, top), Offset(left + boxSize - cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLength), strokeWidth)

        // Bottom Left
        drawLine(cornerColor, Offset(left - strokeWidth/2, top + boxSize), Offset(left + cornerLength, top + boxSize), strokeWidth)
        drawLine(cornerColor, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLength), strokeWidth)

        // Bottom Right
        drawLine(cornerColor, Offset(left + boxSize + strokeWidth/2, top + boxSize), Offset(left + boxSize - cornerLength, top + boxSize), strokeWidth)
        drawLine(cornerColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLength), strokeWidth)
        
        // Red scanning line (animated)
        val currentLineY = top + (boxSize * lineOffset)
        drawLine(
            color = Color.Red,
            start = Offset(left, currentLineY),
            end = Offset(left + boxSize, currentLineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
