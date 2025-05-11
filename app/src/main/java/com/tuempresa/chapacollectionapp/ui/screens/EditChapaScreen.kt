// Archivo: EditChapaScreen.kt
package com.tuempresa.chapacollectionapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.components.OverlayCuadradoConGuiaCircular
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.utils.createImageUri
import com.tuempresa.chapacollectionapp.utils.cropCenterSquare
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeUri
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditChapaScreen(
    chapa: Chapa,
    onSave: (Chapa) -> Unit,
    onCancel: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(chapa.nombre) }
    var pais by remember { mutableStateOf(chapa.pais) }
    var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val scale = remember { mutableStateOf(1f) }
    val imageOffset = remember { mutableStateOf(Offset.Zero) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = cameraImageUri.value
            uri?.let {
                //val inputStream = context.contentResolver.openInputStream(uri)
                //val originalBitmap = BitmapFactory.decodeStream(inputStream)
                //val squareBitmap = cropCenterSquare(originalBitmap)
                //val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                val file = File(context.cacheDir, "chapa_camera_temp.jpg")
                val outputStream = FileOutputStream(file)
                //resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                nuevaImagenUri = uri
                outputStream.close()

                nuevaImagenUri = file.toUri()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val squareBitmap = cropCenterSquare(originalBitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

            val file = File(context.cacheDir, "chapa_gallery_temp.jpg")
            val outputStream = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()

            nuevaImagenUri = file.toUri()
        }
    }

    fun createImageUri(context: Context): Uri {
        val imageFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "chapa_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Editar Chapa", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pais,
            onValueChange = { pais = it },
            label = { Text("Pais") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Galería")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val uri = createImageUri(context)
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Cámara")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val painter = nuevaImagenUri?.let { rememberAsyncImagePainter(it) }
            ?: rememberAsyncImagePainter(File(chapa.imagePath))

        // Tamaño del marco cuadrado visible
        val frameSizeDp = 300.dp
        val density = LocalDensity.current
        val frameSizePx = with(density) { frameSizeDp.toPx() }

        // Estado del zoom y desplazamiento
        val scale = remember { mutableStateOf(1f) }
        val imageOffset = remember { mutableStateOf(Offset.Zero) }

        // Tamaño aproximado de imagen mostrada para limitar desplazamiento
        val imageWidth = 512f  // o el tamaño real si lo conoces
        val imageHeight = 512f

        val maxOffsetX = (((imageWidth * scale.value) - frameSizePx).coerceAtLeast(0f)) / 2f
        val maxOffsetY = (((imageHeight * scale.value) - frameSizePx).coerceAtLeast(0f)) / 2f

        Box(
            modifier = Modifier
                .size(frameSizeDp)
                .clip(RectangleShape)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale.value = (scale.value * zoom).coerceIn(1f, 3f)

                        val maxX = ((imageWidth * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f
                        val maxY = ((imageHeight * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f

                        val newOffset = imageOffset.value + pan
                        imageOffset.value = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                    }
                }
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds, // Esto permite que la imagen se estire con zoom
                modifier = Modifier
                    .size((frameSizePx).dp) // Tamaño real del marco
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        translationX = imageOffset.value.x,
                        translationY = imageOffset.value.y
                    )
            )

            OverlayCuadradoConGuiaCircular(modifier = Modifier.matchParentSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                val finalImagePath = nuevaImagenUri?.let {
                    recortarImagenVisibleDesdeUri(context, it, scale.value, imageOffset.value)?.path
                } ?: chapa.imagePath

                val actualizada = chapa.copy(
                    nombre = nombre,
                    pais = pais,
                    imagePath = finalImagePath
                )
                onSave(actualizada)

                nombre = ""
                pais = ""
                nuevaImagenUri = null

                navController.navigate(Screen.Lista.route) {
                    popUpTo(Screen.Lista.route) { inclusive = false }
                    launchSingleTop = true
                }
            }) {
                Text("Guardar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    }
}
