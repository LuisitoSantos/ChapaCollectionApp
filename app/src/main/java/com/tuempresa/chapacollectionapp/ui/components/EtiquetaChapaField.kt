package com.tuempresa.chapacollectionapp.ui.components

import androidx.compose.runtime.Composable
import com.tuempresa.chapacollectionapp.components.OpcionesSelector

val opcionesEtiquetaChapa = listOf(
    "unknown",
    "Cerveza",
    "Refresco",
    "Zumo",
    "Agua",
    "Sidra",
    "Bebida energética",
    "Bebida isotónica",
    "Té",
    "Café",
    "Vino",
    "Licor",
    "Otra bebida"
)

@Composable
fun EtiquetaChapaField(value: String, onValueChange: (String) -> Unit) {
    OpcionesSelector(
        label = "Etiqueta (opcional)",
        options = opcionesEtiquetaChapa,
        selectedOption = value,
        onOptionSelected = onValueChange
    )
}
