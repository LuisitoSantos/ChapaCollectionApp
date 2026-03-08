// Archivo: AddChapaScreen.kt
package com.tuempresa.chapacollectionapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.components.OverlayCuadradoConGuiaCircular
import com.tuempresa.chapacollectionapp.utils.GeoRepository
import com.tuempresa.chapacollectionapp.utils.createImageUri
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeBitmap
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeUri
import com.tuempresa.chapacollectionapp.utils.rotateBitmapIfRequired
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {

    val chapas by viewModel.allChapas.observeAsState(emptyList())

    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var pais by remember { mutableStateOf(TextFieldValue("")) }
    //var ciudad by remember { mutableStateOf(TextFieldValue("")) }
    var ciudad by remember { mutableStateOf("") }
    var expandedCiudad by remember { mutableStateOf(false) }
    var anio by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val imageUriState = remember { mutableStateOf<Uri?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val scope = rememberCoroutineScope()
    val geoRepository = remember { GeoRepository(context) }
    var sugerencias by remember { mutableStateOf<List<String>>(emptyList()) }


    // Esto inicializa el repositorio de coordenadas sin cambiar la Factory
    LaunchedEffect(Unit) {
        viewModel.inicializarGeo(context)
    }

    // Cada vez que cambie el texto de 'ciudad' o el 'pais', buscamos sugerencias
    LaunchedEffect(ciudad, pais.text) { // Usamos pais.text aquí
        if (ciudad.length >= 2) {
            val results = withContext(Dispatchers.IO) {
                geoRepository.getCitySuggestions(ciudad, pais.text)
            }
            sugerencias = results
        } else {
            sugerencias = emptyList()
        }
    }

    val imageBitmapState = remember { mutableStateOf<Bitmap?>(null) }

    // Si no hay bitmap en memoria, intentamos decodificar desde la URI (para imágenes existentes)
    val decodedBitmap: Bitmap? = imageBitmapState.value ?: imageUriState.value?.let { uri ->
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

    val scale = remember { mutableStateOf(1.2f) }
    val imageOffset = remember { mutableStateOf(Offset.Zero) }

    val cameraImageUri = rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val uri = cameraImageUri.value
                uri?.let {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (originalBitmap != null) {
                            val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
                            //val squareBitmap = cropCenterSquare(rotatedBitmap)
                            // resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                            val file = File(context.cacheDir, "chapa_camera_temp_${System.currentTimeMillis()}.jpg")
                            val outputStream = FileOutputStream(file)
                            val compressOk = rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.close()
                            if (!compressOk) {
                                Toast.makeText(context, "Error al comprimir la imagen", Toast.LENGTH_SHORT).show()
                            }

                            imageUriState.value = file.toUri()
                            imageBitmapState.value = rotatedBitmap
                            // Reset zoom/offset al cargar nueva imagen para evitar desplazamientos heredados
                            scale.value = 1.2f
                            imageOffset.value = Offset.Zero
                        } else {
                            Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error procesando la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
                //val squareBitmap = cropCenterSquare(rotatedBitmap)
                //val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                val file = File(context.cacheDir, "chapa_gallery_temp_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                val compressOk = rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                if (!compressOk) {
                    Toast.makeText(context, "Error al comprimir la imagen", Toast.LENGTH_SHORT).show()
                }

                imageUriState.value = file.toUri()
                imageBitmapState.value = rotatedBitmap
                // Reset zoom/offset al cargar nueva imagen
                scale.value = 1.2f
                imageOffset.value = Offset.Zero
             }
         }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                // createImageUri devuelve una Uri válida
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )
    //Este bloque asegura que al salir de la pantalla, el foco se limpie
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
        }
    }
    //Fin del bloque

    val nombreFocusRequester = remember { FocusRequester() }
    val nombreHasFocus = remember { mutableStateOf(false) }


    val nombresExistentes = chapas.map { it.nombre }.distinct()
    var expanded by remember { mutableStateOf(false) }

    //para pasar al siguiente campo con boton "siguiente"
    val paisFocusRequester = remember { FocusRequester() }
    val ciudadFocusRequester = remember { FocusRequester() }
    val anioFocusRequester = remember { FocusRequester() } // Necesitarás este para saltar desde ciudad

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
                expanded = false
            }
    ){
        // Hacemos el contenido desplazable verticalmente para que los botones siempre sean accesibles
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
            Text("Nueva Chapa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 120.dp))
            Spacer(modifier = Modifier.height(8.dp))


            //Campo Nombre con sugerencias de valores ya introducidos anteriormente
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                // Usamos un TextField normal y renderizamos las sugerencias en un Surface justo debajo
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { incoming ->
                            // Limitar longitud de nombre
                            val maxName = 40
                            val raw = incoming.text
                            val newText = if (raw.length <= maxName) raw else raw.take(maxName)

                            // Si el cambio es una eliminación (longitud menor que antes), preservamos
                            // la selección/composición entrantes para que el usuario pueda seguir borrando
                            val wasDeletion = newText.length < nombre.text.length
                            nombre = if (wasDeletion) {
                                TextFieldValue(text = newText, selection = incoming.selection, composition = incoming.composition)
                            } else {
                                TextFieldValue(newText, TextRange(newText.length))
                            }

                            // Abrir sugerencias solo si hay coincidencias relevantes
                            expanded = nombresExistentes.any { it.contains(newText, ignoreCase = true) && it != newText }
                        },
                        label = { Text("Nombre*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nombreFocusRequester)
                            .onFocusChanged { focusState ->
                                nombreHasFocus.value = focusState.isFocused
                                if (!focusState.isFocused) expanded = false
                                else {
                                    val current = nombre.text
                                    expanded = current.isNotBlank() && nombresExistentes.any {
                                        it.contains(
                                            current,
                                            ignoreCase = true
                                        ) && it != current
                                    }
                                }
                            },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { paisFocusRequester.requestFocus() }),
                        singleLine = true
                    )

                    val sugerenciasVisibles = remember(nombre.text) { nombresExistentes.filter { it.contains(nombre.text, ignoreCase = true) && it != nombre.text } }

                    if (expanded && sugerenciasVisibles.isNotEmpty()) {
                        Surface(
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column {
                                sugerenciasVisibles.take(5).forEachIndexed { idx, sugerencia ->
                                    Text(
                                        text = sugerencia,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                nombre = TextFieldValue(
                                                    sugerencia,
                                                    TextRange(sugerencia.length)
                                                )
                                                expanded = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                    if (idx < minOf(sugerenciasVisibles.size - 1, 4)) HorizontalDivider()
                                }
                                // Si hay más de 5 sugerencias, mostramos un hint para indicar scroll (opcional)
                                if (sugerenciasVisibles.size > 5) {
                                    Text(text = "...", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Campo País: desplegable con búsqueda de países (Locale)
            val countryList = remember {
                Locale.getISOCountries().map { cc -> Locale("", cc).displayCountry }.sorted()
            }
            var expandedCountry by remember { mutableStateOf(false) }
            val countrySuggestions = remember(pais.text) { countryList.filter { it.contains(pais.text, ignoreCase = true) } }

            // Usamos OutlinedTextField y renderizamos sugerencias en Surface debajo (no roban foco)
            Column {
                OutlinedTextField(
                    value = pais.text,
                    onValueChange = { new ->
                        pais = TextFieldValue(new)
                        // mostrar sugerencias si hay coincidencias
                        expandedCountry = new.isNotBlank() && countryList.any { it.contains(new, ignoreCase = true) }
                    },
                    label = { Text("País*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(paisFocusRequester)
                        .onFocusChanged { fs ->
                            if (!fs.isFocused) expandedCountry = false
                            else {
                                val current = pais.text
                                expandedCountry = current.isNotBlank() && countryList.any {
                                    it.contains(
                                        current,
                                        ignoreCase = true
                                    )
                                }
                            }
                        },
                    keyboardActions = KeyboardActions(onNext = { ciudadFocusRequester.requestFocus() }),
                    singleLine = true
                )

                val visibles = remember(pais.text) { countrySuggestions }
                if (expandedCountry && visibles.isNotEmpty()) {
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column {
                            visibles.take(5).forEachIndexed { idx, ctry ->
                                Text(
                                    text = ctry,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pais = TextFieldValue(ctry)
                                            expandedCountry = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                                if (idx < minOf(visibles.size - 1, 4)) HorizontalDivider()
                            }
                            if (visibles.size > 5) {
                                Text(text = "...", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // --- CAMPO CIUDAD (Insertar después del bloque de País y antes de Año) ---
            Column {
                /*
                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { new ->
                        ciudad = new
                        // Aquí podrías activar sugerencias de ciudades más adelante
                        expandedCiudad = new.text.isNotBlank()
                    },
                    label = { Text("Ciudad (Opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(ciudadFocusRequester)
                        .onFocusChanged { fs ->
                            if (!fs.isFocused) expandedCiudad = false
                        },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { anioFocusRequester.requestFocus() }
                    ),
                    singleLine = true
                )*/
                CityAutoCompleteField(
                    label = "Ciudad",
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    suggestions = sugerencias,
                    focusRequester = ciudadFocusRequester,
                    onNext = { anioFocusRequester.requestFocus() } // Salta al año al terminar
                )

                // Espacio para sugerencias de ciudades (si decides implementarlas luego)
                if (expandedCiudad && false) { // Cambiar false por tu lógica de sugerencias
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        // Contenido de sugerencias de ciudad similar a los anteriores
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Campo Año: forzar LTR, limitar a 4 dígitos, permitir borrado contínuo y bloquear entrada extra
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = anio,
                    onValueChange = { incoming ->
                        // extraer solo dígitos
                        val rawDigits = incoming.text.filter { ch -> ch.isDigit() }
                        val maxLen = 4
                        val newText = rawDigits.take(maxLen)

                        // detectar borrado vs inserción
                        val wasDeletion = newText.length < anio.text.length

                        anio = if (wasDeletion) {
                            // preservar selección/composición para que el usuario pueda seguir borrando
                            val sel = incoming.selection
                            val safeSel = TextRange(sel.start.coerceAtMost(newText.length), sel.end.coerceAtMost(newText.length))
                            TextFieldValue(text = newText, selection = safeSel, composition = incoming.composition)
                        } else {
                            // inserción: si ya está al máximo, ignorar nuevos caracteres (hay que borrar para seguir)
                            if (anio.text.length >= maxLen && newText.length > anio.text.length) {
                                // mantener estado actual (no añadir más)
                                anio
                            } else {
                                // aceptar inserción y poner cursor al final
                                TextFieldValue(newText, TextRange(newText.length))
                            }
                        }
                    },
                    label = { Text("Año") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(anioFocusRequester),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            // SELECCIÓN DE COLORES
            val coloresDisponibles = listOf("Rojo", "Azul", "Verde", "Amarillo", "Negro", "Blanco", "Plata", "Dorado")

            // Es recomendable mover estas declaraciones de estado al inicio de tu Composable si es posible
            var colorPrimarioSeleccionado by remember { mutableStateOf<String?>(null) }
            var expandedColorPrimario by remember { mutableStateOf(false) }

            var tieneSecundarios by remember { mutableStateOf(false) }
            var colorSec1 by remember { mutableStateOf<String?>(null) }
            var expandedSec1 by remember { mutableStateOf(false) }
            var colorSec2 by remember { mutableStateOf<String?>(null) }
            var expandedSec2 by remember { mutableStateOf(false) }

            // --- COLOR PRINCIPAL ---
            Text("Color Principal*", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = expandedColorPrimario,
                onExpandedChange = { expandedColorPrimario = !expandedColorPrimario },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = colorPrimarioSeleccionado ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Seleccionar color") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedColorPrimario) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedColorPrimario,
                    onDismissRequest = { expandedColorPrimario = false },
                    modifier = Modifier.heightIn(max = 200.dp) // Limita altura y permite scroll
                ) {
                    coloresDisponibles.forEach { color ->
                        DropdownMenuItem(
                            text = { Text(color) },
                            onClick = {
                                colorPrimarioSeleccionado = color
                                expandedColorPrimario = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- INTERRUPTOR SECUNDARIOS ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = tieneSecundarios, onCheckedChange = { tieneSecundarios = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("¿Tiene colores secundarios?", style = MaterialTheme.typography.bodyMedium)
            }

            if (tieneSecundarios) {
                Spacer(modifier = Modifier.height(12.dp))

                // --- COLOR SECUNDARIO 1 ---
                Text("Color Secundario 1*", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedSec1,
                    onExpandedChange = { expandedSec1 = !expandedSec1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = colorSec1 ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Seleccionar color") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSec1) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSec1,
                        onDismissRequest = { expandedSec1 = false },
                        modifier = Modifier.heightIn(max = 200.dp) // Limita altura y permite scroll
                    ) {
                        coloresDisponibles.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    colorSec1 = c
                                    expandedSec1 = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- COLOR SECUNDARIO 2 ---
                Text("Color Secundario 2 (Opcional)", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedSec2,
                    onExpandedChange = { expandedSec2 = !expandedSec2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = colorSec2 ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Ninguno seleccionado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSec2) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSec2,
                        onDismissRequest = { expandedSec2 = false },
                        modifier = Modifier.heightIn(max = 200.dp) // Limita altura y permite scroll
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguno (Limpiar)") },
                            onClick = {
                                colorSec2 = null
                                expandedSec2 = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                        coloresDisponibles.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    colorSec2 = c
                                    expandedSec2 = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Estado", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))

            // Opciones para cada subcampo
            val opcionesForma = listOf("nada", "algo", "moderada", "demasiada")
            val opcionesRayones = listOf("ninguno", "algunos", "demasiados", "excesivos")
            val opcionesMarcas = listOf("ninguna", "algunas", "demasiadas", "excesivas")
            val opcionesOxido = listOf("nada", "poco", "moderado", "demasiado")

            var selectedForma by remember { mutableStateOf<String?>(null) }
            var expandedForma by remember { mutableStateOf(false) }
            var selectedRayones by remember { mutableStateOf<String?>(null) }
            var expandedRayones by remember { mutableStateOf(false) }
            var selectedMarcas by remember { mutableStateOf<String?>(null) }
            var expandedMarcas by remember { mutableStateOf(false) }
            var selectedOxido by remember { mutableStateOf<String?>(null) }
            var expandedOxido by remember { mutableStateOf(false) }

            // Helper para mapear selección a valor 0..3 (0 = mejor)
            fun mapValor(opcion: String?, tipo: String): Int {
                if (opcion == null) return 0
                val idx = when(tipo) {
                    "forma" -> opcionesForma.indexOf(opcion)
                    "rayones" -> opcionesRayones.indexOf(opcion)
                    "marcas" -> opcionesMarcas.indexOf(opcion)
                    "oxido" -> opcionesOxido.indexOf(opcion)
                    else -> -1
                }
                return if (idx >= 0) idx else 0
            }

            // UI: cuatro dropdowns en columna
            Column {
                // --- DEFORMACIÓN ---
                Text("Deformación", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedForma,
                    onExpandedChange = { expandedForma = !expandedForma },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedForma ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForma) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor() // Vincula el menú al campo
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedForma,
                        onDismissRequest = { expandedForma = false },
                        modifier = Modifier.heightIn(max = 200.dp) // Limita altura para evitar saltos arriba
                    ) {
                        opcionesForma.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { selectedForma = o; expandedForma = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- RAYONES ---
                Text("Rayones", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedRayones,
                    onExpandedChange = { expandedRayones = !expandedRayones },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRayones ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRayones) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRayones,
                        onDismissRequest = { expandedRayones = false },
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        opcionesRayones.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { selectedRayones = o; expandedRayones = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- MARCAS ---
                Text("Marcas", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedMarcas,
                    onExpandedChange = { expandedMarcas = !expandedMarcas },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMarcas ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMarcas) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMarcas,
                        onDismissRequest = { expandedMarcas = false },
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        opcionesMarcas.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { selectedMarcas = o; expandedMarcas = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- ÓXIDO ---
                Text("Óxido", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedOxido,
                    onExpandedChange = { expandedOxido = !expandedOxido },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedOxido ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOxido) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedOxido,
                        onDismissRequest = { expandedOxido = false },
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        opcionesOxido.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o) },
                                onClick = { selectedOxido = o; expandedOxido = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Imagen*", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Galería")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    )
                    if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val uri = createImageUri(context)
                        // createImageUri devuelve una Uri válida
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Cámara")
                }
            }

            imageUriState.value?.let { uri ->

                val frameSizeDp = 300.dp
                val density = LocalDensity.current
                val frameSizePx = with(density) { frameSizeDp.toPx() }

                // Calcular el tamaño mostrado (display) a partir del bitmap real para que
                // los límites de desplazamiento coincidan con el cálculo del recorte.
                val originalWidth = decodedBitmap?.width?.toFloat() ?: 1f
                val originalHeight = decodedBitmap?.height?.toFloat() ?: 1f

                // Mismo cálculo que en recortarImagenVisibleDesdeUri: ContentScale.Fit -> la dimensión mayor ocupa frameSizePx
                val scaleToDisplay = frameSizePx / maxOf(originalWidth, originalHeight)

                val displayedWidth = originalWidth * scaleToDisplay
                val displayedHeight = originalHeight * scaleToDisplay

                Box(
                    modifier = Modifier
                        .size(frameSizeDp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale.value = (scale.value * zoom).coerceIn(1f, 3f)

                                val maxX =
                                    ((displayedWidth * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f
                                val maxY =
                                    ((displayedHeight * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f

                                val newOffset = imageOffset.value + pan
                                imageOffset.value = Offset(
                                    x = newOffset.x.coerceIn(-maxX, maxX),
                                    y = newOffset.y.coerceIn(-maxY, maxY)
                                )
                            }
                        }
                ) {
                    val bmp = decodedBitmap
                    bmp?.let { bmpNonNull ->
                        // Dibujar con Canvas para asegurar correspondencia exacta con el recorte final
                        Canvas(modifier = Modifier.size(frameSizeDp)) {
                            val framePx = frameSizePx
                            val originalW = bmpNonNull.width.toFloat()
                            val originalH = bmpNonNull.height.toFloat()
                            val scaleToDisplayLocal = framePx / maxOf(originalW, originalH)

                            val displayedW = originalW * scaleToDisplayLocal * scale.value
                            val displayedH = originalH * scaleToDisplayLocal * scale.value

                            val initialLeft = (framePx - displayedW) / 2f
                            val initialTop = (framePx - displayedH) / 2f

                            val destLeft = initialLeft + imageOffset.value.x
                            val destTop = initialTop + imageOffset.value.y
                            val destRight = destLeft + displayedW
                            val destBottom = destTop + displayedH

                            val destRect = android.graphics.RectF(destLeft, destTop, destRight, destBottom)
                            val paint = android.graphics.Paint().apply { isFilterBitmap = true; isAntiAlias = true }

                            drawIntoCanvas { canvas ->
                                // Asegurarnos de recortar cualquier dibujo fuera del frame
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.clipRect(0f, 0f, framePx, framePx)
                                canvas.nativeCanvas.drawBitmap(bmpNonNull, null, destRect, paint)
                                canvas.nativeCanvas.restore()
                            }
                        }

                        OverlayCuadradoConGuiaCircular(modifier = Modifier.matchParentSize())
                    }
                 }
             }

            Spacer(modifier = Modifier.height(8.dp))

            // Validación de campos obligatorios: Nombre, País e Imagen
            val isNameValid = remember(nombre.text) { nombre.text.trim().isNotEmpty() }
            val isPaisValid = remember(pais.text) { pais.text.trim().isNotEmpty() }
            val isImageSelected = remember(imageUriState.value, imageBitmapState.value) { imageUriState.value != null || imageBitmapState.value != null }
            // Validación de colores
            val isColorPrimarioValid = remember(colorPrimarioSeleccionado) { (colorPrimarioSeleccionado ?: "").trim().isNotEmpty() }
            val isColorSec1Valid = remember(tieneSecundarios, colorSec1) { !tieneSecundarios || (colorSec1 ?: "").trim().isNotEmpty() }

            // reconstruir missingFields incluyendo colores
            val missingFields = remember(nombre.text, pais.text, imageUriState.value, imageBitmapState.value, colorPrimarioSeleccionado, tieneSecundarios, colorSec1) {
                val list = mutableListOf<String>()
                if (!isNameValid) list.add("Nombre")
                if (!isPaisValid) list.add("País")
                if (!isImageSelected) list.add("Imagen")
                if (!isColorPrimarioValid) list.add("Color Principal")
                if (tieneSecundarios && !isColorSec1Valid) list.add("Color Secundario 1")
                list
            }

            val isFormValid = missingFields.isEmpty()

            if (!isFormValid) {
                Text(
                    text = "Faltan campos: ${missingFields.joinToString(", ")}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row {
                OutlinedButton(
                    onClick = {
                        expanded = false // Cerrar sugerencias antes de navegar
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        // Acción al cancelar: limpiar y volver
                        nombre = TextFieldValue("")
                        pais = TextFieldValue("")
                        anio = TextFieldValue("")
                        imageUriState.value = null

                        navController.navigate(Screen.Lista.route) {
                            popUpTo(Screen.Lista.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                val frameSizePx = with(LocalDensity.current) { 300.dp.toPx() }
                // Calcular el radio de la guía por defecto (mismo que Overlay): marcoSize / 2.2 - stroke/2
                val densityForMask = LocalDensity.current.density
                val guiaGrosorDpForMask = 1f
                val guiaGrosorPxForMask = guiaGrosorDpForMask * densityForMask
                val maskRadiusPxDefault = (frameSizePx / 2.2f) - (guiaGrosorPxForMask / 2f) - 0.5f

                Button(
                    onClick = {
                        // Double-check validation on click (should be enabled only when valid)
                        if (!isFormValid) return@Button

                        expanded = false // Cerrar sugerencias antes de navegar
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)

                        Log.d("AddChapa", "saving image: scale=${scale.value}, offset=${imageOffset.value}, frameSizePx=$frameSizePx, maskRadius=$maskRadiusPxDefault")
                        val finalUri = imageUriState.value?.let { uri ->
                            val bmp = imageBitmapState.value
                            if (bmp != null) {
                                recortarImagenVisibleDesdeBitmap(context, bmp, scale.value, imageOffset.value, frameSizePx, maskRadiusPxDefault)
                            } else {
                                recortarImagenVisibleDesdeUri(context, uri, scale.value, imageOffset.value, frameSizePx, maskRadiusPxDefault)
                            }
                        }
                        val ciudadText = if (ciudad.isBlank()) null else ciudad
                        val anioInt = anio.text.toIntOrNull()
                        // Determinar colores seleccionados
                        val cp = colorPrimarioSeleccionado ?: ""
                        val cs1 = if (tieneSecundarios) colorSec1 else null
                        val cs2 = if (tieneSecundarios) colorSec2 else null

                        // Calcular porcentaje de estado: si no se ha introducido ninguno, dejamos null para indicar 'no definido'
                        val anyStateEntered = listOf(selectedForma, selectedRayones, selectedMarcas, selectedOxido).any { !it.isNullOrBlank() }
                        val estadoPercent = if (!anyStateEntered) {
                            null
                        } else {
                            val vForma = mapValor(selectedForma, "forma")
                            val vRayones = mapValor(selectedRayones, "rayones")
                            val vMarcas = mapValor(selectedMarcas, "marcas")
                            val vOxido = mapValor(selectedOxido, "oxido")
                            val promedio = (vForma + vRayones + vMarcas + vOxido) / 4.0
                            ((1.0 - (promedio / 3.0)) * 100.0).toInt()
                        }

                        viewModel.insertChapa(
                            context,
                            nombre.text,
                            pais.text,
                            ciudadText,
                            finalUri,
                            anioInt,
                            cp,
                            cs1,
                            cs2,
                            selectedForma,
                            selectedRayones,
                            selectedMarcas,
                            selectedOxido,
                            estadoPercent
                        )
                        // Reset form
                        nombre = TextFieldValue("")
                        pais = TextFieldValue("")
                        anio = TextFieldValue("")
                        imageUriState.value = null

                        navController.navigate(Screen.Lista.route) {
                            popUpTo(Screen.Lista.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    enabled = isFormValid
                ) {
                    Text("Añadir Chapa")
                }

            }
        }
    }
}

@Composable
fun CityAutoCompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    focusRequester: FocusRequester,
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
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() })
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
