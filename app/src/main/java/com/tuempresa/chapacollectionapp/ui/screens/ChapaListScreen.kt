package com.tuempresa.chapacollectionapp.ui.screens

//import androidx.activity.result.launch
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.text.color
import coil.compose.AsyncImage
import com.tuempresa.chapacollectionapp.ui.components.ChapaIcon
import com.tuempresa.chapacollectionapp.ui.theme.ChapaBackground
import com.tuempresa.chapacollectionapp.ui.theme.ChapaNavy
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrange
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrangeLight
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOutline
import com.tuempresa.chapacollectionapp.ui.theme.ChapaSlate
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.toIntOrNull

// Fin de imports

private fun valorDeCategoria(chapa: Chapa, categoria: String): String? = when (categoria) {
    "Nombre" -> chapa.nombre
    "País" -> chapa.pais
    "Ciudad" -> chapa.ciudad
    "Año" -> chapa.anio?.takeIf { it > 0 }?.toString()
    "Color primario" -> chapa.colorPrimario
    "Color secundario 1" -> chapa.colorSecundario1
    "Color secundario 2" -> chapa.colorSecundario2
    "Estado de forma" -> chapa.estadoForma
    "Rayones" -> chapa.estadoRayones
    "Marcas" -> chapa.estadoMarcas
    "Óxido" -> chapa.estadoOxido
    "Estado (%)" -> chapa.estadoPercent?.toString()
    "Procedencia" -> chapa.procedencia
    "Método de obtención" -> chapa.metodoObtencion
    "Donante" -> chapa.donante
    "País de obtención" -> chapa.paisObtencion
    "Ciudad de obtención" -> chapa.ciudadObtencion
    else -> null
}?.trim()?.takeIf { it.isNotEmpty() }

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
fun ChapaListScreen(viewModel: ChapaViewModel, authViewModel: AuthViewModel, navController: NavHostController, onLogout: () -> Unit) {

    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    //val chapas by viewModel.allChapas.observeAsState(emptyList())
    val chapas by viewModel.chapasSupabase

    val categoriasDisponibles = listOf(
        "Nombre", "País", "Ciudad", "Año", "Color primario", "Color secundario 1",
        "Color secundario 2", "Estado de forma", "Rayones", "Marcas", "Óxido",
        "Estado (%)", "Procedencia", "Método de obtención", "Donante",
        "País de obtención", "Ciudad de obtención"
    )
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val valoresSeleccionados = remember { mutableStateListOf<String>() }

    val chapaAEditar = remember { mutableStateOf<Chapa?>(null) }
    val chapasEnEdicion = remember { mutableStateOf(setOf<Int>()) }
    var chapaAEliminar by remember { mutableStateOf<Chapa?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var chapaConMenu by remember { mutableStateOf<Chapa?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<String?>(null) }

    val vistaCuadricula = viewModel.vistaCuadricula

    val context = LocalContext.current

    // Aplicar filtro si hay nombre seleccionado
    /*val chapasFiltradas = chapas.filter {
        when (categoriaSeleccionada) {
            "Nombre" -> it.nombre in valoresSeleccionados || valoresSeleccionados.isEmpty()
            "Pais" -> it.pais in valoresSeleccionados || valoresSeleccionados.isEmpty()
            else -> true
        }
    }.sortedBy { it.nombre.lowercase() }

     */

    val chapasFiltradas = chapas.filter { chapa ->
        val queryMatches = query.isBlank() || listOf(chapa.nombre, chapa.pais, chapa.ciudad.orEmpty())
            .any { it.contains(query.trim(), ignoreCase = true) }
        val valorCategoria = categoriaSeleccionada?.let { valorDeCategoria(chapa, it) }
        queryMatches && (
            categoriaSeleccionada == null || valoresSeleccionados.isEmpty() ||
                valorCategoria != null && valorCategoria in valoresSeleccionados
            )
    }.let { lista ->
        // APLICAR ORDENACIÓN SEGÚN EL CRITERIO
        when (viewModel.criterioOrden) {
            "Nombre" -> lista.sortedBy { it.nombre.lowercase() }
            "Pais" -> lista.sortedBy { it.pais.lowercase() }
            "Color" -> lista.sortedBy { it.colorPrimario.lowercase() }
            "Estado" -> lista.sortedByDescending {
                // Extraer número del porcentaje (ej: "90%" -> 90)
                it.estadoPercent ?: 0
            }
            "Año" -> lista.sortedByDescending { it.anio } // Más reciente a más antiguo
            else -> lista.sortedBy { it.nombre.lowercase() }
        }
    }

    val valoresDisponibles = categoriaSeleccionada?.let { categoria ->
        chapas.mapNotNull { valorDeCategoria(it, categoria) }.distinct().sorted()
    }.orEmpty()

    LaunchedEffect(Unit) {
        viewModel.cargarPreferenciaVista(context)
    }

    // Mostrar pantalla de edición si hay una chapa seleccionada
    /*chapaAEditar.value?.let { chapaOriginal -> // Renombramos a chapaOriginal para mayor claridad
        EditChapaScreen(
            chapa = chapaOriginal,
            chapaId = chapaOriginal.id.toString(),
            onSave = { chapaEditada, uriDeImagen ->
                // AHORA PASAMOS LOS 4 PARÁMETROS:
                coroutineScope.launch {
                    viewModel.updateChapaEnSupabase(
                        context = context,
                        chapaOriginal = chapaOriginal,
                        chapaEditada = chapaEditada,
                        nuevaImageUri = uriDeImagen
                    )
                }
                chapaAEditar.value = null
            },
            onCancel = { chapaAEditar.value = null },
            navController = navController,
            viewModel = viewModel
        )
        return
    }*/

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChapaBackground)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    chapasEnEdicion.value = emptySet()
                })
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 16.dp)
            ) {
                FiltroTopBar(
                    modifier = Modifier.align(Alignment.CenterStart),
                    categoriasDisponibles = categoriasDisponibles,
                    valoresDisponibles = valoresDisponibles,
                    valoresSeleccionados = valoresSeleccionados,
                    categoriaSeleccionada = categoriaSeleccionada,
                    onCategoriaSeleccionada = { categoria ->
                        if (categoria != categoriaSeleccionada) valoresSeleccionados.clear()
                        categoriaSeleccionada = categoria
                    },
                    onValorSeleccionado = { valor ->
                        if (valor in valoresSeleccionados) valoresSeleccionados.remove(valor)
                        else valoresSeleccionados.add(valor)
                    },
                    onLimpiarFiltros = {
                        categoriaSeleccionada = null
                        valoresSeleccionados.clear()
                    }
                )
                Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        ChapaIcon(Modifier.fillMaxSize(), color = ChapaOrange)
                        Text(
                            text = chapas.size.toString(),
                            color = ChapaNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = ChapaOrangeLight
                    ) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Opciones de cuenta", tint = ChapaOrange)
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        Text(
                            text = authViewModel.currentUser?.email ?: "Cuenta",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = ChapaSlate
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ordenar por: ${viewModel.criterioOrden}") },
                            onClick = {
                                showMenu = false
                                showSortMenu = true
                            },
                            trailingIcon = { Text("›", color = ChapaSlate) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                showMenu = false
                                authViewModel.signOut()
                                onLogout()
                            },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                        )
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        listOf("Nombre", "Pais", "Color", "Estado", "Año").forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    viewModel.guardarCriterioOrden(context, opcion)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (viewModel.criterioOrden == opcion) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ChapaOrange)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).height(56.dp),
                    singleLine = true,
                    placeholder = { Text("Buscar por nombre o marca…", color = ChapaSlate) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChapaSlate) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F4F8),
                        unfocusedContainerColor = Color(0xFFF0F4F8),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = ChapaNavy,
                        unfocusedTextColor = ChapaNavy
                    )
                )
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = { viewModel.setVistaCuadricula(context, !vistaCuadricula) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, ChapaOutline, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (vistaCuadricula) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = "Cambiar vista",
                        tint = ChapaSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (vistaCuadricula) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItemsIndexed(items = chapasFiltradas, key = { _, item -> item.id ?: item.hashCode() }) { index, chapa ->
                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                onClick = {
                                    imagenSeleccionada = chapa.imagePath
                                },
                                onLongClick = { chapaConMenu = chapa }
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChapaOutline),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color(0xFFF1F3F5))
                                    ) {
                                        AsyncImage(
                                            model = chapa.imagePath,
                                            contentDescription = "Imagen de ${chapa.nombre}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(7.dp)
                                        )
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopStart).padding(5.dp),
                                            shape = RoundedCornerShape(50),
                                            color = ChapaOrangeLight
                                        ) {
                                            Text(
                                                text = "#${(index + 1).toString().padStart(3, '0')}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ChapaOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                                        Text(
                                            text = chapa.nombre,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ChapaNavy,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Public,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = ChapaSlate
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = chapa.pais,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ChapaSlate,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chapa.estadoPercent?.let { "$it%" } ?: "Sin estado",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (chapa.estadoPercent == null) ChapaSlate else ChapaOrange,
                                                maxLines = 1
                                            )
                                            chapa.anio?.takeIf { it > 0 }?.let { year ->
                                                Text(year.toString(), style = MaterialTheme.typography.labelSmall, color = ChapaSlate)
                                            }
                                        }
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = chapaConMenu == chapa,
                                onDismissRequest = { chapaConMenu = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        chapaConMenu = null
                                        chapaAEditar.value = chapa
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        chapaConMenu = null
                                        chapaAEliminar = chapa
                                        mostrarConfirmacion = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                val onEditar: (Chapa) -> Unit = { chapa -> chapaAEditar.value = chapa }
                val onEliminar: (Chapa) -> Unit = { chapa -> chapaAEliminar = chapa; mostrarConfirmacion = true }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(
                        items = chapasFiltradas,
                        key = { _, item -> item.id ?: item.hashCode() }
                    ) { index, chapa ->
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
                                        Icon(imageVector = it, contentDescription = null, modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .size(24.dp))
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
                                    onImageClick = {
                                        imagenSeleccionada = chapa.imagePath
                                    },
                                    number = index + 1
                                )
                            }
                        )
                    }
                }
            }
        }

        if (mostrarConfirmacion && chapaAEliminar != null) {
            AlertDialog(
                onDismissRequest = {
                    mostrarConfirmacion = false
                    chapaAEliminar = null
                },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro de que quieres eliminar esta chapa?") },
                confirmButton = {
                    TextButton(onClick = {
                        chapaAEliminar?.let { viewModel.deleteChapaSupabase(it) }
                        mostrarConfirmacion = false
                        chapaAEliminar = null
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        mostrarConfirmacion = false
                        chapaAEliminar = null
                    }) { Text("Cancelar") }
                }
            )
        }

        // Mantener la lista compuesta por debajo evita volver a crear las tarjetas al cerrar edición.
        chapaAEditar.value?.let { chapaOriginal ->
            EditChapaScreen(
                chapa = chapaOriginal,
                chapaId = chapaOriginal.id.toString(),
                onSave = { _, _ -> chapaAEditar.value = null },
                onCancel = { chapaAEditar.value = null },
                navController = navController,
                viewModel = viewModel
            )
        }

        // Diálogo de imagen ampliada
        //ImageDialog(imageUri = null, imagePath = imagenSeleccionada, onDismiss = { imagenSeleccionada = null })
        imagenSeleccionada?.let { url ->
            ImageDialog(
                imageUrl = url,
                onDismiss = {
                    imagenSeleccionada = null
                }
            )
        }
    }

}
