package com.tuempresa.chapacollectionapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

const val MINIMUM_SPLASH_DURATION_MS = 2_200L

@Composable
fun BeerLoadingScreen(isLoading: Boolean) {
    var logoVisible by remember { mutableFloatStateOf(0f) }
    var beerProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = beerProgress,
        animationSpec = tween(durationMillis = 1_200),
        label = "beer-fill"
    )
    val foamTransition = rememberInfiniteTransition(label = "foam-motion")
    val foamBob by foamTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "foam-bob"
    )

    LaunchedEffect(isLoading) {
        if (isLoading) {
            logoVisible = 0f
            beerProgress = 0f
            delay(180)
            logoVisible = 1f
            delay(420)
            beerProgress = 0.78f
        } else {
            logoVisible = 1f
            beerProgress = 1f
        }
    }

    val background = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onBackground
    val logoColor = Color(0xFF593A12)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 3.dp.toPx()
            val glassBounds = Rect(inset, inset, size.width - inset, size.height - inset)
            val glassPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = glassBounds,
                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                    )
                )
            }
            val beerHeight = size.height * animatedProgress.coerceIn(0f, 1f)
            val beerTop = size.height - beerHeight
            val waveAmplitude = 4.dp.toPx()

            drawPath(glassPath, Color.White.copy(alpha = 0.16f))
            if (beerProgress > 0f) {
                val beerPath = Path().apply {
                    moveTo(0f, beerTop)
                    val steps = 36
                    for (step in 1..steps) {
                        val x = size.width * step / steps
                        val wave = kotlin.math.sin(step * 0.55f + foamBob) * waveAmplitude
                        lineTo(x, beerTop + wave)
                    }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                clipPath(glassPath) {
                    drawPath(beerPath, Color(0xFFE6A52F))

                    // La espuma acompaña la superficie y sube junto con la cerveza.
                    val foamRadius = 10.dp.toPx()
                    val bubbleCount = 16
                    for (index in 0 until bubbleCount) {
                        val x = size.width * (index + 0.5f) / bubbleCount
                        val wave = kotlin.math.sin(index * 0.9f + foamBob) * waveAmplitude
                        val radiusVariation = if (index % 3 == 0) 1.18f else 1f
                        drawCircle(
                            color = Color(0xFFFFF2CE),
                            radius = foamRadius * radiusVariation,
                            center = Offset(x, beerTop + wave)
                        )
                    }
                }
            }

            // Contorno de la copa a pantalla completa.
            drawRoundRect(
                color = foreground.copy(alpha = 0.28f),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(148.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2.1f
                val innerRadius = outerRadius * 0.85f
                val amplitude = (outerRadius - innerRadius) / 2f
                val baseRadius = innerRadius + amplitude
                val teeth = 12
                val steps = teeth * 10
                val labelPath = Path()
                for (step in 0..steps) {
                    val angle = (step.toFloat() / steps) * (2 * Math.PI).toFloat()
                    val radius = baseRadius + amplitude * kotlin.math.cos(angle * teeth)
                    val point = Offset(
                        center.x + radius * kotlin.math.cos(angle),
                        center.y + radius * kotlin.math.sin(angle)
                    )
                    if (step == 0) labelPath.moveTo(point.x, point.y) else labelPath.lineTo(point.x, point.y)
                }
                labelPath.close()
                drawPath(labelPath, Color(0xFFFFF2CE))
            }
            ChapaIcon(
                modifier = Modifier.size(116.dp),
                color = logoColor.copy(alpha = logoVisible)
            )
        }

        Text(
            text = "Cargando colección…",
            color = logoColor.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
