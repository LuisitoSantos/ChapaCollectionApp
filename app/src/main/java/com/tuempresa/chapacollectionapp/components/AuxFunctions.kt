package com.tuempresa.chapacollectionapp.components

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
fun CityAutoCompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    focusRequester: FocusRequester,
    imeAction: ImeAction = ImeAction.Next, // Valor por defecto
    onNext: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.length >= 2 && suggestions.isNotEmpty()
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                // CAMBIO 1: Usar el parámetro imeAction en lugar de ImeAction.Next fijo
                imeAction = imeAction
            ),
            // CAMBIO 2: Definir las acciones para ambos casos (Siguiente y Hecho)
            keyboardActions = KeyboardActions(
                onNext = { onNext() },
                onDone = { onNext() }
            )
        )

        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun procesarImagenParaChapa(bitmap: Bitmap): Bitmap {
    val size = Math.min(bitmap.width, bitmap.height)
    // Usamos Bitmap.Config.ARGB_8888 para permitir la transparencia de las esquinas
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    // IMPORTANTE: Usar el Canvas de android.graphics, no el de Compose
    val canvas = android.graphics.Canvas(output)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }

    val radius = size / 2f

    // 1. Dibujamos un círculo lleno (nuestra máscara)
    canvas.drawCircle(radius, radius, radius, paint)

    // 2. Aplicamos el modo de recorte SRC_IN
    // Esto hace que lo siguiente que dibujemos solo se vea donde ya hay color (el círculo)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    // Calculamos el rectángulo de origen para centrar la imagen si no es cuadrada
    val srcRect = android.graphics.Rect(
        (bitmap.width - size) / 2,
        (bitmap.height - size) / 2,
        (bitmap.width + size) / 2,
        (bitmap.height + size) / 2
    )
    val destRect = android.graphics.Rect(0, 0, size, size)

    canvas.drawBitmap(bitmap, srcRect, destRect, paint)

    // 3. Dibujamos el borde negro (sin xfermode para que se pinte encima)
    paint.xfermode = null
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = android.graphics.Color.BLACK
    paint.strokeWidth = size * 0.04f // Borde del 4% del tamaño

    // Dibujamos el borde ligeramente hacia adentro para que no se corte
    canvas.drawCircle(radius, radius, radius - (paint.strokeWidth / 2), paint)

    return output
}

@Composable
fun AutoCompleteTextField(
    label: String,
    value: TextFieldValue,
    suggestions: List<String>,
    onValueChange: (TextFieldValue) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Filtrar sugerencias basadas en el texto actual
    val filteredSuggestions = remember(value.text, suggestions) {
        if (value.text.length < 1) emptyList()
        else suggestions.filter { it.contains(value.text, ignoreCase = true) }.take(5)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                // Abrir el menú solo si hay texto y hay sugerencias
                expanded = it.text.isNotEmpty() && filteredSuggestions.isNotEmpty()
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            // Sin menuAnchor() ni trailingIcon para que parezca un campo normal
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        // Usamos un DropdownMenu estándar
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Importante para que no robe el foco del teclado mientras escribes
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth(0.9f) // Un poco más estrecho que el campo
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        // Al hacer clic, actualizamos el texto y cerramos
                        onValueChange(TextFieldValue(suggestion, TextRange(suggestion.length)))
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpcionesSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text("Seleccionar") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                options.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            onOptionSelected(opcion)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}