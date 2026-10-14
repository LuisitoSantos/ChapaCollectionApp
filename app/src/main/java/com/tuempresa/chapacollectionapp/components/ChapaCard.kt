package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.ui.theme.ChapaNavy
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrange
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrangeLight
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOutline
import com.tuempresa.chapacollectionapp.ui.theme.ChapaSlate

@Composable
fun ChapaCard(
    chapa: Chapa,
    enEdicion: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    onImageClick: () -> Unit,
    number: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onTap() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ChapaOutline),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (number != null) {
                        Surface(shape = RoundedCornerShape(50), color = ChapaOrangeLight) {
                            Text(
                                text = "#${number.toString().padStart(3, '0')}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = ChapaOrange
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = chapa.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = ChapaNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = ChapaSlate, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(chapa.pais, style = MaterialTheme.typography.bodyMedium, color = ChapaSlate, maxLines = 1)
                    chapa.ciudad?.takeIf { it.isNotBlank() }?.let { ciudad ->
                        Text(" · $ciudad", style = MaterialTheme.typography.bodyMedium, color = ChapaSlate, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (chapa.estadoPercent == null) Color(0xFFF0F4F8) else ChapaOrangeLight
                    ) {
                        Text(
                            text = chapa.estadoPercent?.let { "Estado $it%" } ?: "Estado sin definir",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (chapa.estadoPercent == null) ChapaSlate else ChapaOrange
                        )
                    }
                    chapa.anio?.takeIf { it > 0 }?.let { year ->
                        Spacer(Modifier.width(8.dp))
                        Text("$year", style = MaterialTheme.typography.labelMedium, color = ChapaSlate)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F3F5))
                    .border(1.dp, ChapaOutline, RoundedCornerShape(12.dp))
                    .pointerInput(chapa.imagePath) {
                        detectTapGestures(onTap = { onImageClick() })
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = chapa.imagePath,
                    contentDescription = "Imagen de ${chapa.nombre}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(5.dp)
                )
            }
        }
    }
}
