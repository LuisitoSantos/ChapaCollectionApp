// Archivo: components/OverlayCuadradoConGuiaCircular.kt
package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * Dibuja un marco cuadrado con una guía circular en su interior.
 * Este componente sirve como overlay para alinear visualmente las chapas.
 */
@Composable
fun OverlayCuadradoConGuiaCircular(
    modifier: Modifier = Modifier,
    marcoColor: Color = Color.Black,
    grosorMarco: Dp = 2.dp,
    colorGuia: Color = Color.Red,
    grosorGuia: Dp = 1.dp
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val marcoSize = minOf(canvasWidth, canvasHeight)

        val marcoOffsetX = (canvasWidth - marcoSize) / 2
        val marcoOffsetY = (canvasHeight - marcoSize) / 2

        // Dibuja el borde cuadrado
        drawRoundRect(
            color = marcoColor,
            topLeft = Offset(marcoOffsetX, marcoOffsetY),
            size = Size(marcoSize, marcoSize),
            cornerRadius = CornerRadius.Zero,
            style = Stroke(width = grosorMarco.toPx())
        )

        // Dibuja la guía circular centrada dentro del cuadrado
        drawCircle(
            color = colorGuia,
            radius = marcoSize / 2.2f,
            center = Offset(canvasWidth / 2f, canvasHeight / 2f),
            style = Stroke(width = grosorGuia.toPx())
        )
    }
}
