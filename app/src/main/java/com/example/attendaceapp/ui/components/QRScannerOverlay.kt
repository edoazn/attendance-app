package com.example.attendaceapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun QRScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Scanner frame size
        val scannerSize = canvasWidth * 0.7f
        val left = (canvasWidth - scannerSize) / 2
        val top = (canvasHeight - scannerSize) / 2

        // Dark overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Clear center (scanner area)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // Scanner frame border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f)
        )

        // Corner indicators
        val cornerLength = 80f
        val cornerThickness = 6f

        // Top-left corner
        drawLine(
            color = Color.Green,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = Color.Green,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = cornerThickness
        )

        // Top-right corner
        drawLine(
            color = Color.Green,
            start = Offset(left + scannerSize, top),
            end = Offset(left + scannerSize - cornerLength, top),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = Color.Green,
            start = Offset(left + scannerSize, top),
            end = Offset(left + scannerSize, top + cornerLength),
            strokeWidth = cornerThickness
        )

        // Bottom-left corner
        drawLine(
            color = Color.Green,
            start = Offset(left, top + scannerSize),
            end = Offset(left + cornerLength, top + scannerSize),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = Color.Green,
            start = Offset(left, top + scannerSize),
            end = Offset(left, top + scannerSize - cornerLength),
            strokeWidth = cornerThickness
        )

        // Bottom-right corner
        drawLine(
            color = Color.Green,
            start = Offset(left + scannerSize, top + scannerSize),
            end = Offset(left + scannerSize - cornerLength, top + scannerSize),
            strokeWidth = cornerThickness
        )
        drawLine(
            color = Color.Green,
            start = Offset(left + scannerSize, top + scannerSize),
            end = Offset(left + scannerSize, top + scannerSize - cornerLength),
            strokeWidth = cornerThickness
        )
    }
}
