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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
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

@Composable
fun SearchChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Obtenemos los resultados del ViewModel
    val resultados = viewModel.resultadosBusqueda

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        scale = 1f
        offset = Offset.Zero
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
                    .size(300.dp)
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
                        contentDescription = null,
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Seleccionar Imagen")
            }
        }

        if (imageUri != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        imageUri?.let { uri ->
                            // Generamos el recorte con zoom antes de enviarlo
                            val bitmapProcesado = obtenerBitmapConZoom(context, uri, scale, offset, 300)
                            viewModel.buscarCoincidencias(bitmapProcesado, context)
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
                            .padding(vertical = 4.dp)
                            .clickable { navController.navigate("edit/${chapa.id}") },
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(chapa.imagePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = chapa.nombre, style = MaterialTheme.typography.body1)
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