// Archivo: ui/screens/ChapaListScreen.kt
package com.tuempresa.chapacollectionapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            navController = navController
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
        Column(modifier = Modifier.padding(16.dp)) {

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

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(chapasFiltradas) { chapa ->
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
