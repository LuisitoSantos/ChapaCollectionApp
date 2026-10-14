package com.tuempresa.chapacollectionapp.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale

@Composable
fun FechaObtencionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        label = { Text("Fecha de obtención") },
        placeholder = { Text("Seleccionar fecha") },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                val calendar = Calendar.getInstance()
                value.split('-').takeIf { it.size == 3 }?.let { parts ->
                    val year = parts[0].toIntOrNull()
                    val month = parts[1].toIntOrNull()
                    val day = parts[2].toIntOrNull()
                    if (year != null && month != null && day != null) {
                        calendar.set(year, month - 1, day)
                    }
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onValueChange(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha")
            }
        }
    )
}
