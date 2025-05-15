// Archivo: ui/screens/ChapaListScreen.kt
package com.tuempresa.chapacollectionapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import java.io.File
import com.tuempresa.chapacollectionapp.components.ChapaCard
import com.tuempresa.chapacollectionapp.components.FiltroTopBar
import com.tuempresa.chapacollectionapp.components.ImageDialog

import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChapaListScreen(viewModel: ChapaViewModel, navController: NavHostController) {
    val chapas by viewModel.allChapas.observeAsState(emptyList())

    val categoriasDisponibles = listOf("Nombre", "Pais") // Más adelante puedes añadir "Pais", etc.
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    val valoresSeleccionados = remember { mutableStateListOf<String>() }

    val chapaAEditar = remember { mutableStateOf<Chapa?>(null) }
    val chapasEnEdicion = remember { mutableStateOf(setOf<Int>()) }

    var nombreSeleccionado by remember { mutableStateOf<String?>(null) }
    var imagenSeleccionada by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    var vistaCuadricula by remember { mutableStateOf(false) }


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

    // Contenedor principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { chapasEnEdicion.value = emptySet() })
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Cabecera: Botón + Filtro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vistaCuadricula = !vistaCuadricula }) {
                    Icon(
                        imageVector = if (vistaCuadricula) Icons.Default.ViewList else Icons.Default.GridView, //Importar esots otros iconos: ViewList y GridView
                        contentDescription = "Cambiar vista"
                    )
                }

                // Filtro
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

            Spacer(modifier = Modifier.height(2.dp))

            if (vistaCuadricula) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = chapasFiltradas,
                        key = { it.id } // clave única para que Compose sepa qué item es cuál
                    ) { chapa ->
                        Image(
                            painter = rememberAsyncImagePainter(File(chapa.imagePath)),
                            contentDescription = "Imagen de la chapa",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                .clickable {
                                    imagenSeleccionada = chapa.imagePath
                                }
                        )
                    }
                }
            } else {
                var mostrarConfirmacion by remember { mutableStateOf(false) }
                var chapaAEliminar by remember { mutableStateOf<Chapa?>(null) }
                val dismissStates = remember { mutableStateMapOf<Int, DismissState>() }
                val scope = rememberCoroutineScope()
                val onEditar: (Chapa) -> Unit = { chapa ->
                    chapaAEditar.value = chapa
                }

                val onEliminar: (Chapa) -> Unit = { chapa ->
                    chapaAEliminar = chapa
                    mostrarConfirmacion = true
                }



                LazyColumn {

                    @OptIn(ExperimentalMaterialApi::class)
                    items(chapasFiltradas) { chapa ->
                        val dismissState = rememberDismissState()
                        dismissStates[chapa.id] = dismissState

                        //Fade de los iconos al desplazar tarjeta
                        /*val progress = dismissState.progress.fraction

                        val iconAlpha by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            label = "IconAlpha"
                        )*/

                        LaunchedEffect(dismissState.currentValue) {
                            when {
                                dismissState.isDismissed(DismissDirection.EndToStart) -> {
                                    chapaAEliminar = chapa
                                    mostrarConfirmacion = true
                                }
                                dismissState.isDismissed(DismissDirection.StartToEnd) -> {
                                    onEditar(chapa)
                                    // Restablece el estado después de editar
                                    scope.launch {
                                        dismissState.animateTo(DismissValue.Default)
                                    }
                                }
                            }
                        }

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(
                                DismissDirection.StartToEnd,
                                DismissDirection.EndToStart
                            ),
                            background = {
                                val direction = dismissState.dismissDirection
                                val color = when (direction) {
                                    DismissDirection.StartToEnd -> Color(0xFFBBDEFB) // Azul claro
                                    DismissDirection.EndToStart -> Color(0xFFFFCDD2) // Rojo claro
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
                                        .padding(horizontal = 12.dp, vertical = 4.dp) // Igual que la tarjeta
                                        .clip(RoundedCornerShape(12.dp)) // Mismo shape que la tarjeta
                                        .background(color),
                                    contentAlignment = if (direction == DismissDirection.StartToEnd)
                                        Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .size(24.dp)
                                                //.alpha(iconAlpha) //Fade de los iconos al desplazar tarjeta
                                        )
                                    }
                                }
                            },
                            dismissContent = {
                                ChapaCard(
                                    chapa = chapa,
                                    enEdicion = false, // ya no se usa modo edición por separado
                                    onEditar = { onEditar(chapa) },
                                    onEliminar = { onEliminar(chapa) },
                                    onLongPress = {}, // ya no hace falta
                                    onTap = {},
                                    onImageClick = { imagenSeleccionada = chapa.imagePath }
                                )
                            }
                        )
                        //Spacer(modifier = Modifier.height(4.dp)) // Previene superposición visual
                    }


                    //Antiguo listado de tarjetas con pulsacion larga para mostrar los botones de editar y eliminar
                    /*items(chapasFiltradas) { chapa ->
                        val enEdicion = chapa.id in chapasEnEdicion.value

                        ChapaCard(
                            chapa = chapa,
                            enEdicion = enEdicion,
                            onEditar = { chapaAEditar.value = chapa },
                            onEliminar = {
                                viewModel.deleteChapa(chapa)
                                chapasEnEdicion.value = chapasEnEdicion.value - chapa.id
                            },
                            onLongPress = {
                                chapasEnEdicion.value = chapasEnEdicion.value + chapa.id
                            },
                            onTap = {
                                chapasEnEdicion.value = chapasEnEdicion.value - chapa.id
                            },
                            onImageClick = {
                                imagenSeleccionada = chapa.imagePath
                            }
                        )
                    }*/
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
                                viewModel.deleteChapa(chapaAEliminar!!)
                                val state = dismissStates[chapaAEliminar!!.id]
                                scope.launch {
                                    state?.animateTo(DismissValue.Default)
                                }
                                chapasEnEdicion.value = chapasEnEdicion.value - chapaAEliminar!!.id
                                mostrarConfirmacion = false
                                chapaAEliminar = null
                            }) {
                                Text("Eliminar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                mostrarConfirmacion = false
                                chapaAEliminar?.let {
                                    val state = dismissStates[it.id]
                                    scope.launch {
                                        state?.animateTo(DismissValue.Default)
                                    }
                                }
                                chapaAEliminar = null
                            }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

            }

        }

        // Diálogo de imagen ampliada
        ImageDialog(
            imageUri = null,
            imagePath = imagenSeleccionada,
            onDismiss = { imagenSeleccionada = null }
        )
    }
}
