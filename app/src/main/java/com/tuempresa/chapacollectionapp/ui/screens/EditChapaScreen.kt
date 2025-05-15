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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
import com.tuempresa.chapacollectionapp.utils.rotateBitmapIfRequired
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChapaScreen(
    chapa: Chapa,
    onSave: (Chapa) -> Unit,
    onCancel: () -> Unit,
    navController: NavHostController,
    viewModel: ChapaViewModel // <-- NUEVO
) {
    val chapas by viewModel.allChapas.observeAsState(emptyList())
    val nombresExistentes = chapas.map { it.nombre }.distinct()

    val context = LocalContext.current
    var nombre by remember { mutableStateOf(chapa.nombre) }
    var pais by remember { mutableStateOf(chapa.pais) }
    var anio by remember { mutableStateOf(chapa.anio) }
    var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val scale = remember { mutableStateOf(1.2f) }
    val imageOffset = remember { mutableStateOf(Offset.Zero) }

    //Este bloque asegura que al salir de la pantalla, el foco se limpie
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
        }
    }
    //Fin del bloque

    val keyboardController = LocalSoftwareKeyboardController.current



    val nombreFocusRequester = remember { FocusRequester() }
    val paisFocusRequester = remember { FocusRequester() }

    val nombreHasFocus = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // para sugerencias
    val sugerencias = nombresExistentes.filter {
        it.contains(nombre, ignoreCase = true) && it != nombre
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val uri = cameraImageUri.value
                uri?.let {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (originalBitmap != null) {
                            val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
                            //val squareBitmap = cropCenterSquare(rotatedBitmap)
                            //val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)


                            val file = File(context.cacheDir, "chapa_camera_temp_${System.currentTimeMillis()}.jpg")
                            val outputStream = FileOutputStream(file)
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.close()

                            nuevaImagenUri = file.toUri()
                        } else {
                            Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error procesando la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
            val squareBitmap = cropCenterSquare(rotatedBitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

            val file = File(context.cacheDir, "chapa_gallery_temp_${System.currentTimeMillis()}.jpg")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
                expanded = false
            }
    ){
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Editar Chapa", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            //Campo Nombre con sugerencias de valores ya introducidos anteriormente
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ){
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        expanded = true
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .focusRequester(nombreFocusRequester)
                        .onFocusChanged { focusState ->
                            nombreHasFocus.value = focusState.isFocused
                            if (!focusState.isFocused) expanded = false
                        },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            paisFocusRequester.requestFocus()
                        }
                    ),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded && sugerencias.isNotEmpty(),
                    onDismissRequest = {
                        expanded = false
                        focusManager.clearFocus(force = false)
                    }
                ) {
                    sugerencias.forEach { sugerencia ->
                        DropdownMenuItem(
                            text = { Text(sugerencia) },
                            onClick = {
                                nombre = sugerencia
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pais,
                onValueChange = { pais = it },
                label = { Text("País") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(paisFocusRequester),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = anio.toString(),
                onValueChange = {
                    anio = it.toIntOrNull() ?: 0
                },
                label = { Text("Año") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(2.dp))

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

            // Estado del zoom y desplazamiento (ya estan decalaradas arriba)
            //val scale = remember { mutableStateOf(1f) }
            //val imageOffset = remember { mutableStateOf(Offset.Zero) }

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
                    //contentScale = ContentScale.FillBounds, // Esto permite que la imagen se estire con zoom (deforma la imagen)
                    contentScale = ContentScale.Fit, // o ContentScale.Inside
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
                OutlinedButton(onClick = {
                    expanded = false // Cerrar sugerencias antes de navegar
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    expanded = false // Cerrar sugerencias antes de navegar
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias


                    val uriParaProcesar = nuevaImagenUri ?: File(chapa.imagePath).toUri()

                    val finalImageUri = recortarImagenVisibleDesdeUri(
                        context,
                        uriParaProcesar,
                        scale.value,
                        imageOffset.value
                    )

                    val actualizada = chapa.copy(
                        nombre = nombre,
                        pais = pais,
                        anio = anio,
                        imagePath = finalImageUri?.path ?: chapa.imagePath
                    )
                    onSave(actualizada)

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }) {
                    Text("Guardar")
                }
                /*Button(onClick = {
                    val finalImagePath = nuevaImagenUri?.let {
                        recortarImagenVisibleDesdeUri(context, it, scale.value, imageOffset.value)?.path
                    } ?: chapa.imagePath

                    val actualizada = chapa.copy(
                        nombre = nombre,
                        pais = pais,
                        anio = anio,
                        imagePath = finalImagePath
                    )
                    onSave(actualizada)

                    nombre = ""
                    pais = ""
                    //anio.toString() = ""
                    nuevaImagenUri = null

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }) {
                    Text("Guardar")
                }*/

            }
        }

    }

}
