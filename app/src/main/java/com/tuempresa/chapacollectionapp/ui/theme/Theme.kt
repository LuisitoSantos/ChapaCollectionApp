package com.tuempresa.chapacollectionapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ChapaOrange,
    onPrimary = ChapaNavy,
    secondary = ChapaOrangeLight,
    onSecondary = ChapaNavy,
    tertiary = ChapaSlate,
    background = Color(0xFF221A10),
    onBackground = Color(0xFFF8F7F6),
    surface = Color(0xFF2C261F),
    onSurface = Color(0xFFF8F7F6),
    outline = Color(0xFF60564A),
    outlineVariant = Color(0xFF463E34)
)

private val LightColorScheme = lightColorScheme(
    primary = ChapaOrange,
    onPrimary = Color.White,
    secondary = ChapaSlate,
    onSecondary = Color.White,
    tertiary = ChapaNavy,
    background = ChapaBackground,
    onBackground = ChapaNavy,
    surface = Color.White,
    onSurface = ChapaNavy,
    outline = ChapaOutline,
    outlineVariant = ChapaOutline
)

@Composable
fun ChapaCollectionAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
