// Archivo: AddChapaScreen.kt
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.components.OverlayCuadradoConGuiaCircular
import com.tuempresa.chapacollectionapp.utils.createImageUri
import com.tuempresa.chapacollectionapp.utils.cropCenterSquare
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeUri
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {

    val chapas by viewModel.allChapas.observeAsState(emptyList())

    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var pais by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val imageUriState = remember { mutableStateOf<Uri?>(null) }

    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
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
                    imageUriState.value = uri
                    outputStream.close()

                    imageUriState.value = file.toUri()
                }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val squareBitmap = cropCenterSquare(originalBitmap)
                val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                val file = File(context.cacheDir, "chapa_gallery_temp.jpg")
                val outputStream = FileOutputStream(file)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()

                imageUriState.value = file.toUri()
            }
        }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val scale = remember { mutableStateOf(1f) }
    val imageOffset = remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nueva Chapa", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )

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
                val permissionCheck = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                )
                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val uri = createImageUri(context)
                    cameraImageUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }) {
                Text("Cámara")
            }
        }

        imageUriState.value?.let { uri ->

            val frameSizeDp = 300.dp
            val density = LocalDensity.current
            val frameSizePx = with(density) { frameSizeDp.toPx() }

            val imageWidth = 512f
            val imageHeight = 512f

            val scale = remember { mutableStateOf(1f) }
            val imageOffset = remember { mutableStateOf(Offset.Zero) }

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
                    painter = rememberAsyncImagePainter(uri),
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    val finalUri = imageUriState.value?.let {
                        recortarImagenVisibleDesdeUri(context, it, scale.value, imageOffset.value)
                    }
                    viewModel.insertChapa(context, nombre.text, pais.text, finalUri)
                    nombre = TextFieldValue("")
                    pais = TextFieldValue("")
                    imageUriState.value = null

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Añadir Chapa")
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    // Acción al cancelar: limpiar y volver
                    nombre = TextFieldValue("")
                    pais = TextFieldValue("")
                    imageUriState.value = null

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Cancelar")
            }

        }

    }
}
