package com.tuempresa.chapacollectionapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import androidx.core.content.FileProvider
import java.io.File
import kotlin.text.isNullOrBlank
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun SearchChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tempCameraUri = remember { mutableStateOf<Uri?>(null) }
    var umbralSeleccionado by remember { mutableStateOf(40f) } // Valor inicial del 40%
    var showDialog by remember { mutableStateOf(false) }
    var selectedImagePath by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Limpiamos los resultados y la imagen para que al entrar esté vacío
        viewModel.resultadosBusqueda = emptyList()
        viewModel.estaBuscando = false
        imageUri = null
        // Nota: imageUri = null funciona aquí porque imageUri está definida arriba
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        scale = 1f
        offset = Offset.Zero
    }

    // Launcher para CÁMARA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri.value?.let { uri ->
                imageUri = uri
                scale = 1f
                offset = Offset.Zero
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, ahora lanzamos la cámara
            val uri = createImageUri(context)
            tempCameraUri.value = uri
            cameraLauncher.launch(uri)
        } else {
            // El usuario denegó el permiso, podrías mostrar un mensaje si quieres.
        }
    }

    // Si quieres mostrar la imagen en grande al pulsarla
    // Diálogo de imagen a pantalla completa (estilo Galería)
    if (showDialog && selectedImagePath.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false // Permite que el diálogo ocupe toda la pantalla
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)) // Fondo oscurecido degradado
                    .clickable { showDialog = false }, // Al tocar en cualquier lado se cierra
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = 8.dp,
                    modifier = Modifier
                        .padding(24.dp)
                        .wrapContentSize()
                        .clickable(enabled = false) { } // Evita que el click en la imagen cierre el diálogo
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImagePath),
                        contentDescription = "Vista ampliada",
                        modifier = Modifier
                            .fillMaxWidth(0.95f) // Ocupa el 95% del ancho
                            .wrapContentHeight(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Opcional: Una X pequeña en la esquina superior para indicar cierre
                Text(
                    text = "Toca en cualquier lugar para cerrar",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                )
            }
        }
    }


    // Usamos LazyColumn para que toda la pantalla sea scrolleable si hay muchos resultados
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Buscador de Chapas", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(2.dp, Color.Gray, CircleShape)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale *= zoom
                            scale = scale.coerceIn(0.5f, 5f)
                            offset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Imagen seleccionada para búsqueda",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Marcador de posición de cámara", modifier = Modifier.size(50.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BOTONES DE ORIGEN DE IMAGEN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text("Galería")
                }

                // --> BOTÓN DE CÁMARA CORREGIDO
                Button(
                    onClick = {
                        try {
                            val permission = android.Manifest.permission.CAMERA
                            if (context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val uri = createImageUri(context)
                                tempCameraUri.value = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        } catch (e: Exception) {
                            // Esto evitará que la app se cierre y te dirá el error en el Logcat
                            android.util.Log.e("CAMERA_ERROR", "Error al abrir cámara: ${e.message}")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cámara")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // --- NUEVO: SECCIÓN DEL SLIDER ---
            Text(
                text = "Precisión de búsqueda: ${umbralSeleccionado.toInt()}%",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Slider(
                value = umbralSeleccionado,
                onValueChange = { umbralSeleccionado = it },
                valueRange = 10f..100f, // Rango del 10% al 100%
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )
            Text(
                text = if (umbralSeleccionado < 30f) "Baja (más resultados, menos precisos)"
                else if (umbralSeleccionado > 60f) "Alta (pocos resultados, muy precisos)"
                else "Normal",
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
            if (imageUri != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        imageUri?.let { uri ->
                            val bitmapProcesado = obtenerBitmapConZoom(context, uri, scale, offset, 300)
                            // PASAMOS EL UMBRAL AL VIEWMODEL
                            viewModel.buscarCoincidencias(bitmapProcesado, context, umbralSeleccionado)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.estaBuscando
                ) {
                    if (viewModel.estaBuscando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Buscar Coincidencias")
                    }
                }
            }
        }

        // SECCIÓN DE RESULTADOS
        // 3. LÓGICA DE MENSAJES Y RESULTADOS
        val resultados = viewModel.resultadosBusqueda
        val buscando = viewModel.estaBuscando

        when {
            // CASO A: Se ha buscado y NO hay resultados
            !buscando && imageUri != null && resultados.isEmpty() -> {
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "No se encontraron coincidencias",
                        style = MaterialTheme.typography.h6,
                        color = Color.Gray
                    )
                    Text(
                        text = "Prueba a centrar mejor la chapa o bajar el umbral de similitud.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }

            // CASO B: Hay resultados
            resultados.isNotEmpty() -> {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Text(
                        text = "Coincidencias encontradas (${resultados.size})",
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(resultados) { chapa ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        // SE HA ELIMINADO EL .clickable DE AQUÍ PARA QUE NO CIERRE LA APP
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // IMAGEN (Mantenemos el clic aquí para el diálogo)
                            Image(
                                painter = rememberAsyncImagePainter(chapa.imagePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                                    .clickable {
                                        try {
                                            if (!chapa.imagePath.isNullOrEmpty()) {
                                                selectedImagePath = chapa.imagePath!!
                                                showDialog = true
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("DEBUG_IMAGE", "Error: ${e.message}")
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapa.nombre,
                                    style = MaterialTheme.typography.subtitle1,
                                    color = MaterialTheme.colors.primary
                                )
                                Row {
                                    val paisLabel = if (!chapa.pais.isNullOrBlank()) chapa.pais else "N/A"
                                    val anioLabel = if (chapa.anio != null && chapa.anio.toString().isNotBlank()) " • ${chapa.anio}" else ""

                                    Text(
                                        text = "$paisLabel$anioLabel",
                                        style = MaterialTheme.typography.body2,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun obtenerBitmapConZoom(context: Context, uri: Uri, scale: Float, offset: Offset, sizeDp: Int): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(uri)
    val original = BitmapFactory.decodeStream(inputStream) ?: return null

    // El tamaño visual es 300dp, necesitamos convertirlo a píxeles reales según la densidad del móvil
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()

    // Creamos un bitmap vacío del tamaño del visor
    val resultado = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(resultado)

    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    // Aplicamos las transformaciones (Escala y Traslación)
    val matrix = android.graphics.Matrix()

    // Centramos el bitmap original en el canvas
    val startX = (sizePx - original.width * scale) / 2f + (offset.x * density)
    val startY = (sizePx - original.height * scale) / 2f + (offset.y * density)

    matrix.postScale(scale, scale)
    matrix.postTranslate(startX, startY)

    canvas.drawBitmap(original, matrix, paint)
    return resultado
}

// --> FUNCIÓN AUXILIAR PARA CREAR EL URI (Igual que en AddChapaScreen)
private fun createImageUri(context: android.content.Context): Uri {
    val directory = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Pictures")
    if (!directory.exists()) directory.mkdirs()
    val file = File.createTempFile("chapa_search_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}