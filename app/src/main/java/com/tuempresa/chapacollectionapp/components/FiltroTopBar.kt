package com.tuempresa.chapacollectionapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class FiltroMenu { Cerrado, Categorias, Valores }

@Composable
fun FiltroTopBar(
    modifier: Modifier = Modifier,
    categoriasDisponibles: List<String>,
    valoresDisponibles: List<String>,
    valoresSeleccionados: List<String>,
    categoriaSeleccionada: String?,
    onCategoriaSeleccionada: (String) -> Unit,
    onValorSeleccionado: (String) -> Unit,
    onLimpiarFiltros: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(FiltroMenu.Cerrado) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (categoriaSeleccionada != null || valoresSeleccionados.isNotEmpty()) {
            IconButton(onClick = {
                menuAbierto = FiltroMenu.Cerrado
                onLimpiarFiltros()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Limpiar filtros")
            }
        }

        if (categoriaSeleccionada != null) {
            Box {
                IconButton(onClick = { menuAbierto = FiltroMenu.Valores }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Ver valores del filtro")
                }
                DropdownMenu(
                    expanded = menuAbierto == FiltroMenu.Valores,
                    onDismissRequest = { menuAbierto = FiltroMenu.Cerrado },
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    valoresDisponibles.forEach { valor ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = valor,
                                    style = if (valor in valoresSeleccionados) {
                                        MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    } else MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = { onValorSeleccionado(valor) }
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { menuAbierto = FiltroMenu.Categorias }) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Filtrar por categoría")
            }
            DropdownMenu(
                expanded = menuAbierto == FiltroMenu.Categorias,
                onDismissRequest = { menuAbierto = FiltroMenu.Cerrado },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                categoriasDisponibles.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria) },
                        onClick = {
                            menuAbierto = FiltroMenu.Cerrado
                            onCategoriaSeleccionada(categoria)
                        }
                    )
                }
            }
        }
    }
}
