package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FiltroTopBar(
    categoriasDisponibles: List<String>,
    valoresDisponibles: List<String>,
    valoresSeleccionados: List<String>,
    categoriaSeleccionada: String?,
    onCategoriaSeleccionada: (String) -> Unit,
    onValorSeleccionado: (String) -> Unit,
    onLimpiarFiltros: () -> Unit
) {
    var expandedCategoria by remember { mutableStateOf(false) }
    var expandedValores by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Botón: Limpiar Filtros
        if (categoriaSeleccionada != null || valoresSeleccionados.isNotEmpty()) {
            IconButton(onClick = onLimpiarFiltros) {
                //Text("Limpiar filtros")
                Icon(Icons.Default.Refresh, contentDescription = "Limpiar filtros")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Botón: Valores
        if (categoriaSeleccionada != null) {
            IconButton(onClick = { expandedValores = true }) {
                //Text("Valores")
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Valores")

                DropdownMenu(
                    expanded = expandedValores,
                    onDismissRequest = { expandedValores = false }
                ) {
                    valoresDisponibles.forEach { valor ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = valor,
                                    style = if (valor in valoresSeleccionados) {
                                        MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    }
                                )
                            },
                            onClick = {
                                onValorSeleccionado(valor)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Botón: Filtrar por...
        IconButton(onClick = { expandedCategoria = true }) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Filtrar por")
            Spacer(modifier = Modifier.width(4.dp))
            //Text("Filtrar por...")
            DropdownMenu(
                expanded = expandedCategoria,
                onDismissRequest = { expandedCategoria = false }
            ) {
                categoriasDisponibles.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria) },
                        onClick = {
                            onCategoriaSeleccionada(categoria)
                            expandedCategoria = false
                        }
                    )
                }
            }
        }
    }
}
