// Archivo: EditChapaScreen.kt
package com.tuempresa.chapacollectionapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.components.OverlayCuadradoConGuiaCircular
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.utils.GeoRepository
import com.tuempresa.chapacollectionapp.utils.cropCenterSquare
import com.tuempresa.chapacollectionapp.utils.recortarImagenVisibleDesdeUri
import com.tuempresa.chapacollectionapp.utils.rotateBitmapIfRequired
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tuempresa.chapacollectionapp.utils.createImageUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChapaScreen(
    chapa: Chapa,
    chapaId: Int,
    onSave: (Chapa) -> Unit,
    onCancel: () -> Unit,
    navController: NavHostController,
    viewModel: ChapaViewModel // <-- NUEVO
) {
    val chapas by viewModel.allChapas.observeAsState(emptyList())
    val nombresExistentes = chapas.map { it.nombre }.distinct()
    val context = LocalContext.current

    // 1. Observamos la chapa como un estado de Compose
    val chapaState by viewModel.getChapaById(chapaId).observeAsState()

    // 2. Declaramos las variables de los campos (como las tienes ahora)
    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var pais by remember { mutableStateOf(TextFieldValue("")) }
    var ciudad by remember { mutableStateOf(TextFieldValue("")) }
    var anio by remember { mutableStateOf(TextFieldValue("")) }

    // Variables de estado (Dropdowns)
    var selectedForma by remember { mutableStateOf("") }
    var selectedRayones by remember { mutableStateOf("") }
    var selectedMarcas by remember { mutableStateOf("") }
    var selectedOxido by remember { mutableStateOf("") }

    // Colores (si los tienes)
    var colorPrimarioSeleccionado by remember { mutableStateOf("") }
    var colorSec1 by remember { mutableStateOf("") }
    var colorSec2 by remember { mutableStateOf("") }
    var tieneSecundarios by remember { mutableStateOf(!chapa.colorSecundario1.isNullOrBlank() || !chapa.colorSecundario2.isNullOrBlank()) }
    var scale = remember { mutableStateOf(1.0f) }
    var imageOffset = remember { mutableStateOf(Offset.Zero) }

    //var nombre by remember { mutableStateOf<TextFieldValue>(TextFieldValue(chapa.nombre ?: "")) }
    //var pais by remember(chapa) { mutableStateOf(TextFieldValue(chapa.pais ?: "")) }
    //var ciudad by remember { mutableStateOf(TextFieldValue(chapa.ciudad ?: "")) }
    var expandedCiudad by remember { mutableStateOf(false) }
    // año como TextFieldValue para controlar cursor/selección y limitar a 4 dígitos
    val initialAnioText = if ((chapa.anio ?: 0) > 0) (chapa.anio ?: 0).toString() else ""
    //var anio by remember { mutableStateOf<TextFieldValue>(TextFieldValue(initialAnioText)) }
    var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val geoRepository = remember { GeoRepository(context) }
    var sugerenciasCiudades by remember { mutableStateOf<List<String>>(emptyList()) }

    // 3. EL TRUCO: Cuando 'chapaState' cambie (porque Room detectó el guardado),
    // forzamos la actualización de los campos de texto.
    // 3. EL TRUCO: Cuando 'chapaState' cambie, forzamos la actualización de TODO
    LaunchedEffect(chapaState) {
        chapaState?.let { chapa ->
            // Sincronización de campos de texto (Nombre, País, Ciudad)
            if (nombre.text != (chapa.nombre ?: "")) {
                nombre = TextFieldValue(chapa.nombre ?: "", TextRange(chapa.nombre?.length ?: 0))
            }
            if (pais.text != (chapa.pais ?: "")) {
                pais = TextFieldValue(chapa.pais ?: "", TextRange(chapa.pais?.length ?: 0))
            }
            if (ciudad.text != (chapa.ciudad ?: "")) {
                ciudad = TextFieldValue(chapa.ciudad ?: "", TextRange(chapa.ciudad?.length ?: 0))
            }

            // Sincronización del Año
            val anioBD = if ((chapa.anio ?: 0) > 0) chapa.anio.toString() else ""
            if (anio.text != anioBD) {
                anio = TextFieldValue(anioBD, TextRange(anioBD.length))
            }

            // Sincronización de Selectores (Dropdowns)
            selectedForma = chapa.estadoForma ?: ""
            selectedRayones = chapa.estadoRayones ?: ""
            selectedMarcas = chapa.estadoMarcas ?: ""
            selectedOxido = chapa.estadoOxido ?: ""

            // Sincronización de Colores
            colorPrimarioSeleccionado = chapa.colorPrimario ?: ""
            colorSec1 = chapa.colorSecundario1 ?: ""
            colorSec2 = chapa.colorSecundario2 ?: ""
            tieneSecundarios = !chapa.colorSecundario1.isNullOrBlank()

            // Reset de imagen temporal al cambiar de chapa
            nuevaImagenUri = null
            scale.value = 1.0f
            imageOffset.value = Offset.Zero
        }
    }

    // 4. No mostrar la UI hasta que la chapa cargue por primera vez
    if (chapaState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {



    // Esto inicializa el repositorio de coordenadas sin cambiar la Factory
    LaunchedEffect(Unit) {
        viewModel.inicializarGeo(context)
    }

    // Buscador de sugerencias para la edición
    LaunchedEffect(ciudad.text, pais.text) {
        if (ciudad.text.length >= 2) {
            val results = withContext(Dispatchers.IO) {
                geoRepository.getCitySuggestions(ciudad.text, pais.text)
            }
            sugerenciasCiudades = results
        } else {
            sugerenciasCiudades = emptyList()
        }
    }



    //Este bloque asegura que al salir de la pantalla, el foco se limpie
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
        }
    }
    //Fin del bloque

    val keyboardController = LocalSoftwareKeyboardController.current



    val nombreFocusRequester = remember { FocusRequester() }
    val paisFocusRequester = remember { FocusRequester() }
    val ciudadFocusRequester = remember { FocusRequester() } // <-- NUEVO
    val anioFocusRequester = remember { FocusRequester() }   // <-- NUEVO

    val nombreHasFocus = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // para sugerencias

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
                            //val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)


                            val file = File(context.cacheDir, "chapa_camera_temp_${System.currentTimeMillis()}.jpg")
                            val outputStream = FileOutputStream(file)
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.close()

                            nuevaImagenUri = file.toUri()
                        } else {
                            Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error procesando la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Si se concede el permiso ahora, lanzamos la cámara
            val uri = createImageUri(context)
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }


    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val rotatedBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)
            val squareBitmap = cropCenterSquare(rotatedBitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

            val file = File(context.cacheDir, "chapa_gallery_temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()

            nuevaImagenUri = file.toUri()
        }
    }

    fun createImageUri(context: Context): Uri {
        val imageFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "chapa_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }

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
        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Text("Editar Chapa", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            //Campo Nombre con sugerencias de valores ya introducidos anteriormente
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                // Campo Nombre: OutlinedTextField + lista de sugerencias en Surface debajo (no roba foco)
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { incoming ->
                            val maxName = 40
                            val raw = incoming.text
                            val newText = if (raw.length <= maxName) raw else raw.take(maxName)
                            val wasDeletion = newText.length < nombre.text.length
                            nombre = if (wasDeletion) {
                                TextFieldValue(text = newText, selection = incoming.selection, composition = incoming.composition)
                            } else {
                                TextFieldValue(newText, TextRange(newText.length))
                            }
                            // Mostrar sugerencias sólo si hay coincidencias
                            expanded = nombresExistentes.any { it.contains(newText, ignoreCase = true) && it != newText }
                        },
                        label = { Text("Nombre") },
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
                                if (sugerenciasVisibles.size > 5) {
                                    Text(text = "...", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo País con sugerencias inline (no roba foco)
            val countryList = remember { Locale.getISOCountries().map { cc -> Locale("", cc).displayCountry }.sorted() }
            var expandedCountry by remember { mutableStateOf(false) }

            Column {
                OutlinedTextField(
                    value = pais,
                    onValueChange = { incoming ->
                        pais = incoming
                        expandedCountry = incoming.text.isNotBlank() && countryList.any { it.contains(incoming.text, ignoreCase = true) }
                    },
                    label = { Text("País") },
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { ciudadFocusRequester.requestFocus() }, onDone = { focusManager.clearFocus(); keyboardController?.hide() }),
                    singleLine = true
                )

                val visibles = remember(pais.text) { countryList.filter { it.contains(pais.text, ignoreCase = true) } }
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

            Spacer(modifier = Modifier.height(8.dp))

            // --- CAMPO CIUDAD (Opcional) ---
            Column {
                /*
                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { incoming ->
                        ciudad = incoming
                        // Lógica para futuras sugerencias si las necesitas
                        expandedCiudad = incoming.text.isNotBlank()
                    },
                    label = { Text("Ciudad (Opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(ciudadFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { anioFocusRequester.requestFocus() }
                    ),
                    singleLine = true
                )
                */
                CityAutoCompleteField(
                    label = "Ciudad",
                    value = ciudad.text, // Pasamos solo el string para la lógica del componente
                    onValueChange = { nuevaCiudad ->
                        ciudad = TextFieldValue(nuevaCiudad, TextRange(nuevaCiudad.length))
                    },
                    suggestions = sugerenciasCiudades,
                    focusRequester = ciudadFocusRequester,
                    onNext = { anioFocusRequester.requestFocus() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Campo Año: forzar LTR, limitar a 4 dígitos, permitir borrado contínuo y bloquear entrada extra
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = anio,
                    onValueChange = { incoming ->
                        val rawDigits = incoming.text.filter { ch -> ch.isDigit() }
                        val maxLen = 4
                        val newText = rawDigits.take(maxLen)

                        val wasDeletion = newText.length < anio.text.length

                        anio = if (wasDeletion) {
                            val sel = incoming.selection
                            val safeSel = TextRange(sel.start.coerceAtMost(newText.length), sel.end.coerceAtMost(newText.length))
                            TextFieldValue(text = newText, selection = safeSel, composition = incoming.composition)
                        } else {
                            if (anio.text.length >= maxLen && newText.length > anio.text.length) {
                                anio // ignore extra input
                            } else {
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

            // --- ESTADOS PARA COLORES ---
            val coloresDisponibles = listOf("Rojo", "Azul", "Verde", "Amarillo", "Negro", "Blanco", "Plata", "Dorado", "Bronce", "Naranja")

            // Color Principal: Inicializamos con lo que ya tiene la chapa
            //var colorPrimarioSeleccionado by remember { mutableStateOf(chapa.colorPrimario ?: "") }
            var expandedColorPrimario by remember { mutableStateOf(false) }

            // Colores Secundarios: El switch se activa si ya existe algún color secundario
            //var tieneSecundarios by remember { mutableStateOf(!chapa.colorSecundario1.isNullOrBlank() || !chapa.colorSecundario2.isNullOrBlank()) }
            //var colorSec1 by remember { mutableStateOf(chapa.colorSecundario1 ?: "") }
            var expandedSec1 by remember { mutableStateOf(false) }
            //var colorSec2 by remember { mutableStateOf(chapa.colorSecundario2 ?: "") }
            var expandedSec2 by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))

            // --- UI SELECCIÓN COLOR PRINCIPAL ---
            Text("Color Principal*", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = expandedColorPrimario,
                onExpandedChange = { expandedColorPrimario = !expandedColorPrimario },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = colorPrimarioSeleccionado,
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
                    modifier = Modifier.heightIn(max = 200.dp)
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

            // --- INTERRUPTOR COLORES SECUNDARIOS ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = tieneSecundarios, onCheckedChange = { tieneSecundarios = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("¿Tiene colores secundarios?", style = MaterialTheme.typography.bodyMedium)
            }

            if (tieneSecundarios) {
                Spacer(modifier = Modifier.height(12.dp))

                // --- COLOR SECUNDARIO 1 ---
                Text("Color Secundario 1", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedSec1,
                    onExpandedChange = { expandedSec1 = !expandedSec1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = colorSec1,
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
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        coloresDisponibles.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { colorSec1 = c; expandedSec1 = false },
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
                        value = colorSec2,
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
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguno (Limpiar)") },
                            onClick = { colorSec2 = ""; expandedSec2 = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                        coloresDisponibles.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { colorSec2 = c; expandedSec2 = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // --- Estado (Deformación, Rayones, Marcas, Óxido) ---
            Text("Estado", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            val opcionesForma = listOf("nada", "algo", "moderada", "demasiada")
            val opcionesRayones = listOf("ninguno", "algunos", "demasiados", "excesivos")
            val opcionesMarcas = listOf("ninguna", "algunas", "demasiadas", "excesivas")
            val opcionesOxido = listOf("nada", "poco", "moderado", "demasiado")

            // Aseguramos que si vienen nulos de la base de datos, el TextField no falle
            //var selectedForma by remember { mutableStateOf(chapa.estadoForma ?: "") }
            var expandedForma by remember { mutableStateOf(false) }
            //var selectedRayones by remember { mutableStateOf(chapa.estadoRayones ?: "") }
            var expandedRayones by remember { mutableStateOf(false) }
            //var selectedMarcas by remember { mutableStateOf(chapa.estadoMarcas ?: "") }
            var expandedMarcas by remember { mutableStateOf(false) }
            //var selectedOxido by remember { mutableStateOf(chapa.estadoOxido ?: "") }
            var expandedOxido by remember { mutableStateOf(false) }

            fun mapValor(opcion: String?, tipo: String): Int {
                if (opcion == null) return 0
                val idx = when (tipo) {
                    "forma" -> opcionesForma.indexOf(opcion)
                    "rayones" -> opcionesRayones.indexOf(opcion)
                    "marcas" -> opcionesMarcas.indexOf(opcion)
                    "oxido" -> opcionesOxido.indexOf(opcion)
                    else -> -1
                }
                return if (idx >= 0) idx else 0
            }

            // Deformación
            Text("Deformación", style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = expandedForma,
                onExpandedChange = { expandedForma = !expandedForma }
            ) {
                OutlinedTextField(
                    value = selectedForma,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Seleccionar") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForma) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor() // IMPORTANTE: Vincula el menú al campo
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedForma,
                    onDismissRequest = { expandedForma = false },
                    modifier = Modifier.heightIn(max = 200.dp) // Evita que salte arriba
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

            // Rayones
            Text("Rayones", style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = expandedRayones,
                onExpandedChange = { expandedRayones = !expandedRayones }
            ) {
                OutlinedTextField(
                    value = selectedRayones,
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

            // Marcas
            Text("Marcas", style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = expandedMarcas,
                onExpandedChange = { expandedMarcas = !expandedMarcas }
            ) {
                OutlinedTextField(
                    value = selectedMarcas,
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

            // Óxido
            Text("Óxido", style = MaterialTheme.typography.bodyMedium)
            ExposedDropdownMenuBox(
                expanded = expandedOxido,
                onExpandedChange = { expandedOxido = !expandedOxido }
            ) {
                OutlinedTextField(
                    value = selectedOxido,
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

            Row {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Galería")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {

                    val permissionCheckResult = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )

                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        // Si ya tenemos permiso, abrimos directamente
                        val uri = createImageUri(context)
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        // Si no, pedimos el permiso
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Cámara")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Usa chapaState (la verdad de la base de datos) en lugar de chapa (el valor estático)
            val painter = nuevaImagenUri?.let { rememberAsyncImagePainter(it) }
                ?: chapaState?.imagePath?.let { path ->
                    rememberAsyncImagePainter(File(path))
                }
                ?: rememberAsyncImagePainter(null)

            // Tamaño del marco cuadrado visible
            val frameSizeDp = 300.dp
            val density = LocalDensity.current
            val frameSizePx = with(density) { frameSizeDp.toPx() }

            // Estado del zoom y desplazamiento (ya estan decalaradas arriba)
            //val scale = remember { mutableStateOf(1f) }
            //val imageOffset = remember { mutableStateOf(Offset.Zero) }

            // Tamaño aproximado de imagen mostrada para limitar desplazamiento
            val imageWidth = 512f  // o el tamaño real si lo conoces
            val imageHeight = 512f

            Box(
                modifier = Modifier
                    .size(frameSizeDp)
                    .clip(RectangleShape)
                    .background(Color.LightGray) // Útil para ver el fondo si la imagen no es cuadrada
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale.value = (scale.value * zoom).coerceIn(1f, 3f)

                            // Cálculo dinámico para evitar que la imagen se salga del marco al moverla
                            val maxX =
                                ((frameSizePx * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f
                            val maxY =
                                ((frameSizePx * scale.value) - frameSizePx).coerceAtLeast(0f) / 2f

                            val newOffset = imageOffset.value + pan
                            imageOffset.value = Offset(
                                x = newOffset.x.coerceIn(-maxX, maxX),
                                y = newOffset.y.coerceIn(-maxY, maxY)
                            )
                        }
                    }
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // IMPORTANTE: Crop asegura que la imagen llene el cuadrado inicial
                    modifier = Modifier
                        .fillMaxSize() // Ocupa todo el Box de 300.dp
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = imageOffset.value.x,
                            translationY = imageOffset.value.y
                        )
                )

                OverlayCuadradoConGuiaCircular(modifier = Modifier.matchParentSize())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                OutlinedButton(onClick = {
                    expanded = false // Cerrar sugerencias antes de navegar
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias

                    onCancel()

                    /*
                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }*/
                    navController.popBackStack(Screen.Lista.route, inclusive = false)
                }) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    expanded = false // Cerrar sugerencias antes de navegar
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true) // <-- Esto limpia el foco y oculta sugerencias


                    val uriParaProcesar = nuevaImagenUri ?: chapa.imagePath?.let { File(it).toUri() }
                    if (uriParaProcesar == null) {
                        Toast.makeText(context, "No hay imagen disponible para procesar", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalImageUri = recortarImagenVisibleDesdeUri(
                        context,
                        uriParaProcesar,
                        scale.value,
                        imageOffset.value,
                        frameSizePx
                    )

                    // calcular estadoPercent
                    val anyStateEntered = listOf(selectedForma, selectedRayones, selectedMarcas, selectedOxido).any { !it.isNullOrBlank() }
                    val estadoPercentCalc = if (!anyStateEntered) {
                        null
                    } else {
                        val vf = mapValor(selectedForma, "forma")
                        val vr = mapValor(selectedRayones, "rayones")
                        val vm = mapValor(selectedMarcas, "marcas")
                        val vo = mapValor(selectedOxido, "oxido")
                        val prom = (vf + vr + vm + vo) / 4.0
                        ((1.0 - (prom / 3.0)) * 100.0).toInt()
                    }

                    val actualizada = chapa.copy(
                        nombre = nombre.text,
                        pais = pais.text,
                        ciudad = if (ciudad.text.isBlank()) null else ciudad.text,
                        anio = anio.text.toIntOrNull() ?: 0,
                        imagePath = finalImageUri?.path ?: chapa.imagePath,
                        colorPrimario = colorPrimarioSeleccionado ?: "",
                        colorSecundario1 = if (tieneSecundarios) colorSec1 else null,
                        colorSecundario2 = if (tieneSecundarios) colorSec2 else null,
                        estadoForma = selectedForma,
                        estadoRayones = selectedRayones,
                        estadoMarcas = selectedMarcas,
                        estadoOxido = selectedOxido,
                        estadoPercent = estadoPercentCalc
                    )
                    viewModel.updateChapa(actualizada)
                    onSave(actualizada)

                    /*
                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }*/
                    navController.popBackStack(Screen.Lista.route, inclusive = false)
                }) {
                    Text("Guardar")
                }
                /*Button(onClick = {
                    val finalImagePath = nuevaImagenUri?.let {
                        recortarImagenVisibleDesdeUri(context, it, scale.value, imageOffset.value)?.path
                    } ?: chapa.imagePath

                    val actualizada = chapa.copy(
                        nombre = nombre,
                        pais = pais,
                        anio = anio,
                        imagePath = finalImagePath
                    )
                    onSave(actualizada)

                    nombre = ""
                    pais = ""
                    //anio.toString() = ""
                    nuevaImagenUri = null

                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }) {
                    Text("Guardar")
                }*/
            }
        }
    }
    }
}


