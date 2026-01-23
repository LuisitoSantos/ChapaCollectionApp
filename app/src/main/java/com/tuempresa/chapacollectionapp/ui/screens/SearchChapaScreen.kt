package com.tuempresa.chapacollectionapp.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel

@Composable
fun SearchChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

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

            // ... (Mantén aquí el Box circular con el Image que ya tienes)
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
                    onClick = { viewModel.buscarCoincidencias(imageUri!!) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) {
                    Text("Buscar Coincidencias", color = Color.White)
                }
            }
        }

        // SECCIÓN DE RESULTADOS
        if (resultados.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Text(
                    text = "Se han encontrado ${resultados.size} coincidencias",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            items(resultados) { chapa ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            // Navegar al detalle o edición de la chapa encontrada
                            navController.navigate("edit_chapa/${chapa.id}")
                        },
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(chapa.imagePath),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            androidx.compose.material3.Text(text = chapa.nombre, style = MaterialTheme.typography.subtitle1)
                            Text(text = "ID: ${chapa.id}", style = MaterialTheme.typography.caption)
                        }
                    }
                }
            }
        }
    }
}