package com.tuempresa.chapacollectionapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.ui.theme.ChapaBackground
import com.tuempresa.chapacollectionapp.ui.theme.ChapaNavy
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrange
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOutline
import com.tuempresa.chapacollectionapp.ui.theme.ChapaSlate
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File


@Composable
fun SearchChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var umbralSeleccionado by remember { mutableStateOf(85f) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tempCameraUri = remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.resultadosBusqueda = emptyList()
        viewModel.estaBuscando = false
        imageUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
        hasSearched = false
        scale = 1f
        offset = Offset.Zero
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempCameraUri.value
            hasSearched = false
            scale = 1f
            offset = Offset.Zero
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            tempCameraUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Cómo buscar una chapa") },
            text = { Text("Fotografía o selecciona una imagen, ajusta el encuadre y pulsa «Buscar coincidencias». Puedes cambiar la precisión para obtener más o menos resultados.") },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Entendido") }
            }
        )
    }

    if (showImageDialog && !imageUri?.toString().isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showImageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)).clickable { showImageDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ChapaBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Buscador de Chapas",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = ChapaNavy
                )
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Ayuda", tint = ChapaNavy)
                }
            }
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(270.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(246.dp)) {
                    drawCircle(
                        color = ChapaOrange.copy(alpha = 0.42f),
                        radius = size.minDimension / 2f,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(10.dp.toPx(), 8.dp.toPx())
                            )
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF17202A))
                        .border(3.dp, ChapaOrange, CircleShape)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Imagen elegida para buscar",
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(50),
                    color = ChapaOrange
                ) {
                    Text(
                        if (imageUri == null) "LISTO PARA ESCANEAR" else "AJUSTA EL ENCUADRE",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = ChapaNavy
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(82.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFECEFF3),
                        contentColor = ChapaNavy
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ChapaOrange)
                        Text("Galería", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Button(
                    onClick = {
                        val permission = android.Manifest.permission.CAMERA
                        if (context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = createImageUri(context)
                            tempCameraUri.value = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    modifier = Modifier.weight(1f).height(82.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChapaOrange, contentColor = ChapaNavy)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text("Cámara", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = ChapaOrange)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Precisión de búsqueda",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = ChapaNavy
                    )
                    Text(
                        "${umbralSeleccionado.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChapaOrange
                    )
                }
                Slider(
                    value = umbralSeleccionado,
                    onValueChange = { umbralSeleccionado = it },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = ChapaOrange,
                        activeTrackColor = ChapaOrange,
                        inactiveTrackColor = ChapaOutline
                    )
                )
                Text(
                    "Ajusta para filtrar por coincidencia exacta",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ChapaSlate,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (imageUri != null) {
            item {
                Button(
                    onClick = {
                        imageUri?.let { uri ->
                            val bitmap = obtenerBitmapConZoom(context, uri, scale, offset, 300)
                            hasSearched = true
                            viewModel.buscarCoincidencias(bitmap, context, umbralSeleccionado)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !viewModel.estaBuscando,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChapaOrange, contentColor = Color.White)
                ) {
                    if (viewModel.estaBuscando) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Buscar coincidencias", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        val resultados = viewModel.resultadosBusqueda
        if (resultados.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Coincidencias encontradas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = ChapaNavy)
                    Text("${resultados.size} resultados", style = MaterialTheme.typography.bodyMedium, color = ChapaSlate)
                }
            }
            items(resultados) { chapa ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ChapaOutline),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = chapa.imagePath,
                            contentDescription = "Imagen de ${chapa.nombre}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF1F3F5))
                                .clickable { showImageDialog = true }
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chapa.nombre, style = MaterialTheme.typography.titleMedium, color = ChapaNavy, maxLines = 1)
                            Text(
                                listOfNotNull(chapa.pais, chapa.anio?.takeIf { it > 0 }?.toString()).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChapaSlate
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = ChapaSlate)
                    }
                }
            }
        } else if (hasSearched && imageUri != null && !viewModel.estaBuscando) {
            item {
                Text(
                    "Todavía no hay coincidencias. Prueba a ajustar el encuadre o la precisión.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChapaSlate,
                    textAlign = TextAlign.Center
                )
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
