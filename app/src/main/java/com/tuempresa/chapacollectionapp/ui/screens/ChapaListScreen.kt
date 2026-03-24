package com.tuempresa.chapacollectionapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.rememberDismissState

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex

import androidx.navigation.NavHostController

import coil.compose.rememberAsyncImagePainter

import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.components.ChapaCard
import com.tuempresa.chapacollectionapp.components.FiltroTopBar
import com.tuempresa.chapacollectionapp.components.ImageDialog

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.io.File

// Material3
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext

// Fin de imports

// Composable que dibuja un contorno tipo "chapa" con dientes regulares (triángulos)
@Composable
fun ChapaOutline(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidthDp: Dp = 1.dp,
    teeth: Int = 20
) {
    Canvas(modifier = modifier) {
        val baseStroke = strokeWidthDp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val center = Offset(cx, cy)

        // Base radius where teeth start
        val outerAvailable = min(w, h) / 2f - baseStroke

        // Tooth geometry (más pequeño para evitar zonas gruesas)
        // Hacemos los dientes un poco más largos y el radio de la punta algo mayor
        // Incrementamos ligeramente las dimensiones para mejorar la legibilidad
        val toothLen = outerAvailable * 0.22f
        val innerRadius = outerAvailable - toothLen * 0.5f
        val tipRadius = outerAvailable + toothLen * 0.35f

        val toothAngle = (2 * PI / teeth).toFloat()
        val halfBaseAngle = toothAngle * 0.18f // base estrecha para puntas más definidas

        // Grosores finos y uniformes (más delgados para mantener los huecos visibles)
        val toothStroke = (baseStroke * 0.3f).coerceAtLeast(0.6f)
        val rimStroke = (baseStroke * 0.45f).coerceAtLeast(0.6f)
        val outerStroke = (baseStroke * 0.45f).coerceAtLeast(0.6f)

        // Precalcular puntos de puntas e interiores
        val tipPoints = Array(teeth) { Offset.Zero }
        val innerPoints = Array(teeth) { Offset.Zero }
        for (i in 0 until teeth) {
            val angle = i * toothAngle
            tipPoints[i] = Offset(cx + tipRadius * cos(angle), cy + tipRadius * sin(angle))
            innerPoints[i] = Offset(cx + innerRadius * cos(angle), cy + innerRadius * sin(angle))
        }

        // Dibujar los dientes como dos líneas (lado izquierdo y lado derecho) hacia la punta
        for (i in 0 until teeth) {
            val angle = i * toothAngle
            val leftAngle = angle - halfBaseAngle
            val rightAngle = angle + halfBaseAngle

            val leftInner = Offset(cx + innerRadius * cos(leftAngle), cy + innerRadius * sin(leftAngle))
            val rightInner = Offset(cx + innerRadius * cos(rightAngle), cy + innerRadius * sin(rightAngle))
            val tip = tipPoints[i]

            // Líneas finas desde el borde interior hasta la punta (evita triángulos cerrados gruesos)
            drawLine(color = color, start = leftInner, end = tip, strokeWidth = toothStroke, cap = StrokeCap.Round)
            drawLine(color = color, start = rightInner, end = tip, strokeWidth = toothStroke, cap = StrokeCap.Round)
        }

        // Contorno exterior: unir las puntas con un trazo fino
        val tipPath = Path()
        for (i in tipPoints.indices) {
            val p = tipPoints[i]
            if (i == 0) tipPath.moveTo(p.x, p.y) else tipPath.lineTo(p.x, p.y)
        }
        tipPath.close()
        drawPath(path = tipPath, color = color, style = Stroke(width = outerStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Borde interior circular (rim) más fino
        drawCircle(
            color = color,
            center = center,
            radius = innerRadius - baseStroke * 0.6f,
            style = Stroke(width = rimStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChapaListScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    val chapas by viewModel.allChapas.observeAsState(emptyList())

    val categoriasDisponibles = listOf("Nombre", "Pais")
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val valoresSeleccionados = remember { mutableStateListOf<String>() }

    val chapaAEditar = remember { mutableStateOf<Chapa?>(null) }
    val chapasEnEdicion = remember { mutableStateOf(setOf<Int>()) }

    var imagenSeleccionada by remember { mutableStateOf<String?>(null) }

    val vistaCuadricula = viewModel.vistaCuadricula

    val context = LocalContext.current
    viewModel.cargarPreferenciaVista(context)

    // Aplicar filtro si hay nombre seleccionado
    val chapasFiltradas = chapas.filter {
        when (categoriaSeleccionada) {
            "Nombre" -> it.nombre in valoresSeleccionados || valoresSeleccionados.isEmpty()
            "Pais" -> it.pais in valoresSeleccionados || valoresSeleccionados.isEmpty()
            else -> true
        }
    }.sortedBy { it.nombre.lowercase() }

    val valoresDisponibles = when (categoriaSeleccionada) {
        "Nombre" -> chapas.map { it.nombre }.distinct().sorted()
        "Pais" -> chapas.map { it.pais }.distinct().sorted()
        else -> emptyList()
    }

    // Mostrar pantalla de edición si hay una chapa seleccionada
    chapaAEditar.value?.let { chapa ->
        EditChapaScreen(
            chapa = chapa,
            chapaId = chapa.id,
            onSave = {
                viewModel.updateChapa(it)
                chapaAEditar.value = null
            },
            onCancel = { chapaAEditar.value = null },
            navController = navController,
            viewModel = viewModel
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { chapasEnEdicion.value = emptySet() }) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Cabecera: usar un Box para mantener el contador siempre centrado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(56.dp) // altura fija para evitar cambios al desplegar filtros
            ) {
                // Row con extremos (izquierda: icono, derecha: filtros)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.setVistaCuadricula(context, !viewModel.vistaCuadricula)
                    }) {
                        Icon(
                            imageVector = if (vistaCuadricula) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "Cambiar vista"
                        )
                    }

                    Box(modifier = Modifier.wrapContentWidth()) {
                        FiltroTopBar(
                            categoriasDisponibles = categoriasDisponibles,
                            valoresDisponibles = valoresDisponibles,
                            valoresSeleccionados = valoresSeleccionados,
                            categoriaSeleccionada = categoriaSeleccionada,
                            onCategoriaSeleccionada = { categoriaSeleccionada = it },
                            onValorSeleccionado = { valor ->
                                if (valoresSeleccionados.contains(valor)) {
                                    valoresSeleccionados.remove(valor)
                                } else {
                                    valoresSeleccionados.add(valor)
                                }
                            },
                            onLimpiarFiltros = {
                                categoriaSeleccionada = null
                                valoresSeleccionados.clear()
                            }
                        )
                    }
                }

                // Contador centrado por encima del Row (overlay estable)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val hayFiltro = categoriaSeleccionada != null || valoresSeleccionados.isNotEmpty()

                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        ChapaOutline(
                            modifier = Modifier.fillMaxSize(),
                            color = if (hayFiltro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                            strokeWidthDp = 1.dp,
                            teeth = 16
                        )

                        Text(
                            text = chapasFiltradas.size.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (hayFiltro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (vistaCuadricula) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = chapasFiltradas, key = { it.id }) { chapa ->
                        Box(modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { imagenSeleccionada = chapa.imagePath }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(chapa.imagePath ?: "")),
                                contentDescription = "Imagen de la chapa",
                                // 1. Usamos Fit para ver el borde original de la imagen sin estirarlo
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )

                            // Badge porcentaje: 'ND' si null, o 'xx%'
                            Box(modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val text = chapa.estadoPercent?.let { "$it%" } ?: "ND"
                                Text(text = text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                var mostrarConfirmacion by remember { mutableStateOf(false) }
                var chapaAEliminar by remember { mutableStateOf<Chapa?>(null) }
                val onEditar: (Chapa) -> Unit = { chapa -> chapaAEditar.value = chapa }
                val onEliminar: (Chapa) -> Unit = { chapa -> chapaAEliminar = chapa; mostrarConfirmacion = true }

                LazyColumn {
                    items(
                        items = chapasFiltradas,
                        key = { it.id } // Usar 'key' es crucial para animaciones correctas
                    ) { chapa ->
                        val dismissState = rememberDismissState(
                            confirmStateChange = {
                                if (it == DismissValue.DismissedToEnd) {
                                    // Dirección para EDITAR
                                    onEditar(chapa)
                                    return@rememberDismissState false // No dejar que el item desaparezca
                                } else if (it == DismissValue.DismissedToStart) {
                                    // Dirección para ELIMINAR
                                    onEliminar(chapa) // Abre el diálogo de confirmación
                                    return@rememberDismissState false // No dejar que el item desaparezca
                                }
                                true
                            }
                        )

                        // ESTE BLOQUE SE HA ELIMINADO: Ya no necesitas el LaunchedEffect aquí
                        // porque la lógica está dentro de confirmStateChange.

                        SwipeToDismiss(
                            state = dismissState,
                            modifier = Modifier.animateItemPlacement(), // Animación suave al eliminar
                            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                            background = {
                                val direction = dismissState.dismissDirection
                                val color = when (direction) {
                                    DismissDirection.StartToEnd -> Color(0xFFBBDEFB) // Azul para editar
                                    DismissDirection.EndToStart -> Color(0xFFFFCDD2) // Rojo para eliminar
                                    else -> Color.Transparent
                                }
                                val icon = when (direction) {
                                    DismissDirection.StartToEnd -> Icons.Default.Edit
                                    DismissDirection.EndToStart -> Icons.Default.Delete
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color),
                                    contentAlignment = if (direction == DismissDirection.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    icon?.let {
                                        Icon(imageVector = it, contentDescription = null, modifier = Modifier.padding(horizontal = 16.dp).size(24.dp))
                                    }
                                }
                            },
                            dismissContent = {
                                ChapaCard(
                                    chapa = chapa,
                                    enEdicion = false,
                                    onEditar = { onEditar(chapa) },
                                    onEliminar = { onEliminar(chapa) },
                                    onLongPress = {},
                                    onTap = {},
                                    onImageClick = { imagenSeleccionada = chapa.imagePath }
                                )
                            }
                        )
                    }
                }


                if (mostrarConfirmacion && chapaAEliminar != null) {
                    AlertDialog(
                        onDismissRequest = {
                            // Al cancelar tocando fuera, simplemente cierra el diálogo
                            mostrarConfirmacion = false
                            chapaAEliminar = null
                        },
                        title = { Text("Confirmar eliminación") },
                        text = { Text("¿Estás seguro de que quieres eliminar esta chapa?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteChapa(chapaAEliminar!!)
                                mostrarConfirmacion = false
                                chapaAEliminar = null
                            }) { Text("Eliminar") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                // Al cancelar, simplemente cierra el diálogo
                                mostrarConfirmacion = false
                                chapaAEliminar = null
                            }) { Text("Cancelar") }
                        }
                    )
                }

            }
        }

        // Diálogo de imagen ampliada
        ImageDialog(imageUri = null, imagePath = imagenSeleccionada, onDismiss = { imagenSeleccionada = null })
    }
}
