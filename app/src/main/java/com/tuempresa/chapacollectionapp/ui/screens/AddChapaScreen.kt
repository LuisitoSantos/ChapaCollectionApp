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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.util.UUID
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.components.OverlayCuadradoConGuiaCircular
import com.tuempresa.chapacollectionapp.utils.createImageUri
import com.tuempresa.chapacollectionapp.utils.cropCenterSquare
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeUri
import com.tuempresa.chapacollectionapp.utils.rotateBitmapIfRequired
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {

    val chapas by viewModel.allChapas.observeAsState(emptyList())

    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var pais by remember { mutableStateOf(TextFieldValue("")) }
    var anio by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val imageUriState = remember { mutableStateOf<Uri?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val bitmap by remember(imageUriState.value) {
        mutableStateOf(
            imageUriState.value?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        )
    }

    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

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
                            // resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                            val file = File(context.cacheDir, "chapa_camera_temp_${System.currentTimeMillis()}.jpg")
                            val outputStream = FileOutputStream(file)
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.close()

                            imageUriState.value = file.toUri()
                        } else {
                            Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error procesando la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
                //val squareBitmap = cropCenterSquare(rotatedBitmap)
                //val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                val file = File(context.cacheDir, "chapa_gallery_temp_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
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
    //Este bloque asegura que al salir de la pantalla, el foco se limpie
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
        }
    }
    //Fin del bloque

    val nombreFocusRequester = remember { FocusRequester() }
    val nombreHasFocus = remember { mutableStateOf(false) }

    val scale = remember { mutableStateOf(1.2f) }
    val imageOffset = remember { mutableStateOf(Offset.Zero) }
    val nombresExistentes = chapas.map { it.nombre }.distinct()
    var expanded by remember { mutableStateOf(false) }
    val sugerencias = nombresExistentes.filter {
        it.contains(nombre.text, ignoreCase = true) && it != nombre.text
    }

    //para pasar al siguiente campo con boton "siguiente"
    val paisFocusRequester = remember { FocusRequester() }

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
            Text("Nueva Chapa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 120.dp))
            Spacer(modifier = Modifier.height(8.dp))


            //Campo Nombre con sugerencias de valores ya introducidos anteriormente
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        expanded = true
                    },
                    label = { Text("Nombre*") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .focusRequester(nombreFocusRequester)// Cierra el menú al perder el foco
                        .onFocusChanged { focusState ->// Cierra el menú al perder el foco
                            nombreHasFocus.value = focusState.isFocused// Cierra el menú al perder el foco
                            if (!focusState.isFocused) {// Cierra el menú al perder el foco
                                expanded = false // Cierra el menú al perder el foco
                            }
                        },// Cierra el menú al perder el foco
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            paisFocusRequester.requestFocus() // Salta al campo "País"
                        }
                    ),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded && sugerencias.isNotEmpty(),
                    onDismissRequest = {
                        expanded = false //Cierra el menú también al tocar fuera
                        focusManager.clearFocus(force = false)}
                ) {
                    sugerencias.forEach { sugerencia ->
                        DropdownMenuItem(
                            text = { Text(sugerencia) },
                            onClick = {
                                nombre = TextFieldValue(sugerencia)
                                expanded = false

                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = pais,
                onValueChange = { pais = it },
                label = { Text("Pais*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(paisFocusRequester),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = anio,
                onValueChange = { anio = it },
                label = { Text("Año") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Color Principal*", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            //Text("Color Secundario 1", style = MaterialTheme.typography.titleMedium)
            //Spacer(modifier = Modifier.height(2.dp))
            //Text("Color Secundario 2", style = MaterialTheme.typography.titleMedium)
            //Spacer(modifier = Modifier.height(2.dp))
            Text("Estado", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            //Text("Forma", style = MaterialTheme.typography.titleMedium)
            //Spacer(modifier = Modifier.height(2.dp))
            //Text("Rayones", style = MaterialTheme.typography.titleMedium)
            //Spacer(modifier = Modifier.height(2.dp))
            //Text("Marcas", style = MaterialTheme.typography.titleMedium)
            //Spacer(modifier = Modifier.height(2.dp))
            //Text("Oxido", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))

            Divider(thickness = 1.dp, color = Color.LightGray)

            Spacer(modifier = Modifier.height(2.dp))
            Text("Imagen*", style = MaterialTheme.typography.titleMedium)
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
                    bitmap?.let {
                        Image(
                            /*painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .memoryCacheKey(UUID.randomUUID().toString()) //fuerza recarga de la imagen
                                    .crossfade(true)
                                    .build()

                            ),*/
                            painter = BitmapPainter(it.asImageBitmap()),
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedButton(
                    onClick = {
                        expanded = false // Cerrar sugerencias antes de navegar
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias
                        // Acción al cancelar: limpiar y volver
                        nombre = TextFieldValue("")
                        pais = TextFieldValue("")
                        anio = TextFieldValue("")
                        imageUriState.value = null

                        navController.navigate(Screen.Lista.route) {
                            popUpTo(Screen.Lista.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        expanded = false // Cerrar sugerencias antes de navegar
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias
                        val finalUri = imageUriState.value?.let {
                            recortarImagenVisibleDesdeUri(context, it, scale.value, imageOffset.value)
                        }
                        val anioInt = anio.text.toIntOrNull()
                        viewModel.insertChapa(context, nombre.text, pais.text, finalUri, anioInt)
                        nombre = TextFieldValue("")
                        pais = TextFieldValue("")
                        anio = TextFieldValue("")
                        imageUriState.value = null

                        navController.navigate(Screen.Lista.route) {
                            popUpTo(Screen.Lista.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Añadir Chapa")
                }

            }

        }

    }
}
