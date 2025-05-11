// components/ChapaCard.kt
package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.data.Chapa
import java.io.File

@Composable
fun ChapaCard(
    chapa: Chapa,
    enEdicion: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onTap() }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = chapa.nombre, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = chapa.pais, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.width(80.dp)) //Separacion entre testo e imagen chapa. Si separo mas, se montan los botones de edicion y eliminar
                    Image(
                        painter = rememberAsyncImagePainter(File(chapa.imagePath)),
                        contentDescription = "Imagen de la chapa",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                            .clickable { onImageClick() }
                    )
                }


                if (enEdicion) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = { onEditar() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onEliminar() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }
}
