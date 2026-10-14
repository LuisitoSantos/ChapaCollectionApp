package com.tuempresa.chapacollectionapp.ui.components

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.chapacollectionapp.R
import com.tuempresa.chapacollectionapp.ui.theme.ChapaNavy
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrange
import com.tuempresa.chapacollectionapp.ui.theme.ChapaBackground
import kotlinx.coroutines.delay

const val MINIMUM_SPLASH_DURATION_MS = 2_200L

private object BeerPourSound {
    private var player: MediaPlayer? = null
    private var lastStartedAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun play(context: android.content.Context) {
        if (player?.isPlaying == true) return
        val now = SystemClock.elapsedRealtime()
        if (lastStartedAt != 0L && now - lastStartedAt < 3_000L) return
        lastStartedAt = now
        release()
        player = MediaPlayer.create(context.applicationContext, R.raw.beer_pour)?.also { audio ->
            audio.setOnCompletionListener {
                if (player === it) player = null
                it.release()
            }
            audio.start()
            // Mantiene el vertido al comienzo y baja el volumen al acercarse la espuma.
            for (step in 1..12) {
                handler.postDelayed({
                    if (player === audio) {
                        val volume = (1f - step / 12f).coerceAtLeast(0f)
                        runCatching { audio.setVolume(volume, volume) }
                        if (step == 12) stop()
                    }
                }, 1_000L + step * 100L)
            }
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        release()
    }

    private fun release() {
        player?.let { audio ->
            player = null
            runCatching { if (audio.isPlaying) audio.stop() }
            audio.release()
        }
    }
}

@Composable
fun BeerLoadingScreen(isLoading: Boolean) {
    val context = LocalContext.current
    var logoVisible by remember { mutableFloatStateOf(0f) }
    var beerProgress by remember { mutableFloatStateOf(0f) }
    val animatedBeerProgress by animateFloatAsState(
        targetValue = beerProgress,
        animationSpec = tween(durationMillis = 260, easing = LinearEasing),
        label = "beer-fill"
    )
    val bubbleTransition = rememberInfiniteTransition(label = "bubble-motion")
    val bubbleProgress by bubbleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble-progress"
    )

    LaunchedEffect(isLoading) {
        if (isLoading) {
            BeerPourSound.play(context)
            logoVisible = 0f
            beerProgress = 0f
            delay(180)
            while (beerProgress < 0.9f) {
                beerProgress = (beerProgress + 0.03f).coerceAtMost(0.9f)
                delay(45)
            }
        } else {
            BeerPourSound.stop()
            beerProgress = 1f
        }
        logoVisible = 1f
    }

    Box(
        modifier = Modifier.fillMaxSize().background(ChapaBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val beerTop = size.height * (1f - animatedBeerProgress.coerceIn(0f, 1f))
            val waveAmplitude = 5.dp.toPx()
            val beerPath = Path().apply {
                moveTo(0f, beerTop)
                val steps = 64
                for (step in 0..steps) {
                    val x = size.width * step / steps
                    val wave = kotlin.math.sin(step * 0.22f + bubbleProgress * 5f) * waveAmplitude
                    lineTo(x, beerTop + wave)
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(beerPath, ChapaOrange)

            // Pequeñas burbujas ascienden dentro del líquido hasta la superficie.
            val bubbleCount = 24
            for (index in 0 until bubbleCount) {
                val phase = (bubbleProgress + index * 0.173f) % 1f
                val column = (index * 7 % bubbleCount).toFloat() / (bubbleCount - 1)
                val x = size.width * (0.04f + column * 0.92f)
                val liquidHeight = size.height * animatedBeerProgress.coerceIn(0f, 1f)
                val y = size.height - phase * liquidHeight
                val radius = (2.2f + (index * 13 % 7) * 0.8f).dp.toPx()
                val alpha = 0.18f + (index % 4) * 0.055f
                val bubbleColor = Color(0xFFFFF4D9)

                if (y >= beerTop && animatedBeerProgress > 0f) {
                    drawCircle(
                        color = bubbleColor.copy(alpha = alpha * 0.5f),
                        radius = radius,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = bubbleColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(x, y),
                        style = Stroke(width = (0.7f).dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.9f),
                        radius = radius * 0.22f,
                        center = Offset(x - radius * 0.32f, y - radius * 0.34f)
                    )
                }
            }

            // Espuma irregular y luminosa sobre la superficie ascendente.
            if (animatedBeerProgress > 0f) {
                val foamCount = 28
                for (index in 0 until foamCount) {
                    val x = size.width * index / (foamCount - 1)
                    val wave = kotlin.math.sin(index * 0.72f + bubbleProgress * 5f) * waveAmplitude
                    val radius = (3f + (index * 11 % 6) * 1.1f).dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFF2CE).copy(alpha = 0.7f),
                        radius = radius,
                        center = Offset(x, beerTop + wave)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = radius * 0.24f,
                        center = Offset(x - radius * 0.25f, beerTop + wave - radius * 0.28f)
                    )
                }
            }
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
                val labelPath = androidx.compose.ui.graphics.Path()
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
                color = ChapaNavy.copy(alpha = logoVisible)
            )
        }

        Text(
            text = "Cargando colección…",
            color = ChapaNavy.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
        )
    }
}
