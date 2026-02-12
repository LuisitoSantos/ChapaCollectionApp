// components/ImageDialog.kt
package com.tuempresa.chapacollectionapp.components

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import java.io.File

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ImageDialog(imageUri: Uri?, imagePath: String?, onDismiss: () -> Unit) {
    if (imageUri == null && imagePath == null) return


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // <- esto es clave
    ) {

        // Control de animación de visibilidad
        var visible by remember { mutableStateOf(false) }

        // Lanzar animación justo al mostrar el diálogo
        LaunchedEffect(Unit) {
            visible = true
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)) // Fondo más suave y translúcido
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(imagePath)),
                    contentDescription = "Vista ampliada de la chapa",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Para mantener proporción cuadrada si es posible
                        .padding(16.dp)
                )
            }

        }
    }

}


