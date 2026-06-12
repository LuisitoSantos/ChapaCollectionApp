package com.tuempresa.chapacollectionapp.ui.components // Ajusta al nombre de tu paquete

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/*
@Composable
fun ChapaIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    teeth: Int = 21 // Número real de dientes de una chapa estándar
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2.1f
        val innerRadius = outerRadius * 0.82f // El círculo interno de la chapa
        val toothDepth = outerRadius * 0.12f

        val path = Path()

        // Dibujamos el zigzag de los 21 dientes
        for (i in 0 until (teeth * 2)) {
            val angle = (i.toFloat() * Math.PI / teeth).toFloat()
            val radius = if (i % 2 == 0) outerRadius else outerRadius - toothDepth

            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // 1. Fondo sutil de la chapa
        drawPath(
            path = path,
            color = color.copy(alpha = 0.08f),
            style = Fill
        )

        // 2. Borde exterior (los dientes)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Círculo interno (el relieve de la chapa)
        drawCircle(
            color = color,
            radius = innerRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}
 */
/*
@Composable
fun ChapaIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    teeth: Int = 12 // <--- Bajamos de 21 a 12 para que se noten más
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2.1f
        val innerRadius = outerRadius * 0.80f
        val toothDepth = outerRadius * 0.18f // Un poco más profundo para resaltar

        val path = Path()

        for (i in 0 until (teeth * 2)) {
            val angle = (i.toFloat() * Math.PI / teeth).toFloat()
            // Zigzag entre radio exterior e interior
            val radius = if (i % 2 == 0) outerRadius else outerRadius - toothDepth

            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Relleno sutil
        drawPath(path = path, color = color.copy(alpha = 0.1f), style = Fill)

        // Borde exterior (los dientes)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Círculo interno (el borde de la tapa)
        drawCircle(
            color = color,
            radius = innerRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}
 */

@Composable
fun ChapaIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    teeth: Int = 12
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2.1f
        val innerRadius = outerRadius * 0.85f

        // La diferencia entre el punto más alto y el más bajo de la onda
        val amplitude = (outerRadius - innerRadius) / 2
        val baseRadius = innerRadius + amplitude

        val path = androidx.compose.ui.graphics.Path()

        // Aumentamos los pasos para que la curva sea suave
        val steps = teeth * 10
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * (2 * Math.PI).toFloat()

            // Usamos la función Coseno para crear una ondulación suave en lugar de picos
            val currentRadius = baseRadius + amplitude * kotlin.math.cos(angle * teeth)

            val x = center.x + currentRadius * kotlin.math.cos(angle)
            val y = center.y + currentRadius * kotlin.math.sin(angle)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Relleno sutil del cuerpo
        drawPath(
            path = path,
            color = color.copy(alpha = 0.1f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Borde exterior redondeado (usamos un trazo grueso para suavizar aún más)
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // Círculo interno (el relieve de la parte plana)
        drawCircle(
            color = color,
            radius = innerRadius * 0.95f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    }
}
