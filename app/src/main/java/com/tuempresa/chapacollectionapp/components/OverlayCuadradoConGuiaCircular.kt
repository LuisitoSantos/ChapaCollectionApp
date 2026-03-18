// Archivo: components/OverlayCuadradoConGuiaCircular.kt
package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
/*
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
*/

@Composable
fun OverlayCuadradoConGuiaCircular(modifier: Modifier = Modifier) {
    // Usamos el color de fondo de tu app para ocultar las esquinas (ej. Surface)
    val colorFondo = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        // El mismo radio que usas para el recorte
        val density = this.density
        val radius = (canvasWidth / 2.2f) - (1f * density / 2f) - 0.5f

        // 1. Dibujar el fondo que tapa las esquinas (un rectángulo con un agujero circular)
        val path = androidx.compose.ui.graphics.Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
            addOval(androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius))
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        drawPath(path, color = colorFondo)

        // 2. Dibujar el borde negro circular
        drawCircle(
            color = androidx.compose.ui.graphics.Color.Black,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}