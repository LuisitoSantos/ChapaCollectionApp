package com.tuempresa.chapacollectionapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.tuempresa.chapacollectionapp.ui.theme.ChapaCollectionAppTheme
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModelFactory
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.data.ChapaDatabase
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.repository.ChapaRepository
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.unit.IntOffset

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ChapaDatabase.getDatabase(this)
        val repository = ChapaRepository(database.chapaDao())
        val factory = ChapaViewModelFactory(repository)

        setContent {
            ChapaCollectionAppTheme {
                val navController = rememberNavController()
                val viewModel: ChapaViewModel = viewModel(factory = factory)

                Scaffold(
                    bottomBar = {
                        BottomNavigation {
                            val currentRoute =
                                navController.currentBackStackEntryAsState().value?.destination?.route
                            listOf(Screen.Lista, Screen.Anadir).forEach { screen ->
                                BottomNavigationItem(
                                    selected = currentRoute == screen.route,
                                    onClick = { navController.navigate(screen.route) },
                                    label = { Text(screen.label) },
                                    icon = {}
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Lista.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Lista.route) {
                            ChapaListScreen(viewModel, navController)
                        }
                        composable(Screen.Anadir.route) {
                            AddChapaScreen(viewModel, navController)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ChapaListScreen(viewModel: ChapaViewModel, navController: NavHostController) {
        val chapas by viewModel.allChapas.observeAsState(emptyList())

        val chapasOrdenadas = chapas.sortedBy { it.nombre.lowercase() }
        var nombre by remember { mutableStateOf(TextFieldValue("")) }
        var descripcion by remember { mutableStateOf(TextFieldValue("")) }
        var modoEdicion by remember { mutableStateOf(false) }
        var textoBusqueda by remember { mutableStateOf("") }
        var filtroActivo by remember { mutableStateOf<String?>(null) } // "Nombre", "Descripción", etc.
        val valoresFiltroSeleccionados = remember { mutableStateOf(setOf<String>()) }
        var menuTipoAbierto by remember { mutableStateOf(false) }
        //var menuValorAbierto by remember { mutableStateOf(false) }

        val menuCategoriaAbierto = remember { mutableStateOf(false) }
        val menuValorAbierto = remember { mutableStateOf(false) }

        val valoresDisponibles = remember { mutableStateOf<List<String>>(emptyList()) }

        val context = LocalContext.current
        val imageUriState = remember { mutableStateOf<Uri?>(null) }
        var chapaEnModoEdicion by remember { mutableStateOf<Int?>(null) }
        val chapasEnEdicion = remember { mutableStateOf(setOf<Int>()) }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri -> imageUriState.value = uri }
        )
        val imageUri: Uri? = imageUriState.value
        var chapaAEditar by remember { mutableStateOf<Chapa?>(null) }

        val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    imageUriState.value = cameraImageUri.value
                }
            }

        val galleryLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                imageUriState.value = uri
            }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    val uri = createImageUri(context)
                    cameraImageUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        )
        val chapasFiltradas = chapas.filter { chapa ->
            when (filtroActivo) {
                "Nombre" -> valoresFiltroSeleccionados.value.isEmpty() || valoresFiltroSeleccionados.value.contains(
                    chapa.nombre
                )

                "Descripción" -> valoresFiltroSeleccionados.value.isEmpty() || valoresFiltroSeleccionados.value.contains(
                    chapa.descripcion
                )

                else -> true
            }
        }.sortedBy { it.nombre.lowercase() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        chapasEnEdicion.value = emptySet()
                    })
                }
        ) {

            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // Botón de valores solo si hay una categoría activa
                    if (filtroActivo != null) {
                        // Icono de limpiar
                        IconButton(onClick = {
                            filtroActivo = null
                            valoresFiltroSeleccionados.value = emptySet()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Limpiar filtros")
                        }

                        Button(
                            onClick = { menuValorAbierto.value = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Valores")
                            Spacer(modifier = Modifier.width(2.dp))
                            //Text("Valores")
                            DropdownMenu(
                                expanded = menuValorAbierto.value,
                                onDismissRequest = { menuValorAbierto.value = false }

                            ) {
                                valoresDisponibles.value.forEach { valor ->
                                    val seleccionado = valor in valoresFiltroSeleccionados.value
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = valor,
                                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            valoresFiltroSeleccionados.value = if (seleccionado) {
                                                valoresFiltroSeleccionados.value - valor
                                            } else {
                                                valoresFiltroSeleccionados.value + valor
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón de filtro con icono y texto
                    Button(
                        onClick = { menuCategoriaAbierto.value = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Filtrar")
                        Spacer(modifier = Modifier.width(2.dp))
                        //Text(filtroActivo ?: "Filtrar por...")
                        // Menú de categorías
                        DropdownMenu(
                            expanded = menuCategoriaAbierto.value,
                            onDismissRequest = { menuCategoriaAbierto.value = false }
                        ) {
                            listOf("Nombre", "Descripción").forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria) },
                                    onClick = {
                                        filtroActivo = categoria
                                        valoresFiltroSeleccionados.value =
                                            emptySet() // Limpiar valores seleccionados
                                        menuCategoriaAbierto.value = false
                                        // Set valores disponibles según categoría seleccionada
                                        valoresDisponibles.value = when (categoria) {
                                            "Nombre" -> chapas.map { it.nombre }.distinct()
                                            "Descripción" -> chapas.map { it.descripcion }
                                                .distinct()

                                            else -> emptyList()
                                        }
                                    }
                                )
                            }
                        }
                    }


                }



                if (chapaAEditar != null) {
                    EditChapaScreen(
                        chapa = chapaAEditar!!,
                        onSave = { chapaActualizada ->
                            viewModel.updateChapa(chapaActualizada)
                            chapaAEditar = null
                        },
                        onCancel = {
                            chapaAEditar = null
                        },
                        navController = navController
                    )
                    return // Evita que se renderice el resto
                }

                var mostrarMenu by remember { mutableStateOf(false) }
                var nombreSeleccionado by remember { mutableStateOf<String?>(null) }

                val chapasFiltradas = chapas
                    .filter {
                        when (filtroActivo) {
                            "Nombre" -> valoresFiltroSeleccionados.value.isEmpty() || valoresFiltroSeleccionados.value.contains(
                                it.nombre
                            )

                            "Descripción" -> valoresFiltroSeleccionados.value.isEmpty() || valoresFiltroSeleccionados.value.contains(
                                it.descripcion
                            )

                            else -> true
                        }
                    }
                    .sortedBy { it.nombre.lowercase() } // Puede cambiar esto según el criterio de orden que prefieras

                LazyColumn {
                    items(chapasFiltradas) { chapa ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            chapaEnModoEdicion = chapa.id
                                        },
                                        onTap = {
                                            chapaEnModoEdicion = null
                                        }
                                    )
                                }
                        ) {
                            val enEdicion = chapa.id in chapasEnEdicion.value
                            // Tarjeta de la chapa
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .pointerInput(chapa.id) {
                                        detectTapGestures(
                                            onLongPress = {
                                                chapasEnEdicion.value =
                                                    chapasEnEdicion.value + chapa.id
                                            },
                                            onTap = {
                                                // Solo salir del modo edición si ya estaba en modo edición
                                                if (chapa.id in chapasEnEdicion.value) {
                                                    chapasEnEdicion.value =
                                                        chapasEnEdicion.value - chapa.id
                                                }
                                            }
                                        )
                                    },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    // Contenido de la tarjeta
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = chapa.nombre,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = chapa.descripcion,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Image(
                                            painter = rememberAsyncImagePainter(File(chapa.imagePath)),
                                            contentDescription = "Imagen de la chapa",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .padding(8.dp)
                                        )
                                    }

                                    // Botones flotantes en la esquina superior derecha
                                    if (enEdicion) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            IconButton(onClick = { chapaAEditar = chapa }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar"
                                                )
                                            }
                                            IconButton(onClick = {
                                                viewModel.deleteChapa(chapa)
                                                chapasEnEdicion.value =
                                                    chapasEnEdicion.value - chapa.id
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar"
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

    }

    @Composable
    fun EditChapaScreen(
        chapa: Chapa,
        onSave: (Chapa) -> Unit,
        onCancel: () -> Unit,
        navController: NavHostController
    ) {
        val context = LocalContext.current
        var nombre by remember { mutableStateOf(chapa.nombre) }
        var descripcion by remember { mutableStateOf(chapa.descripcion) }
        var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
        val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    val uri = cameraImageUri.value
                    uri?.let {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        val squareBitmap = cropCenterSquare(originalBitmap)
                        val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                        val file = File(context.cacheDir, "chapa_camera_temp.jpg")
                        val outputStream = FileOutputStream(file)
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()

                        nuevaImagenUri = file.toUri()
                    }
                }
            }

        /*val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            nuevaImagenUri.value = uri
        }*/

        val galleryLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    val squareBitmap = cropCenterSquare(originalBitmap)
                    val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                    val file = File(context.cacheDir, "chapa_temp.jpg")
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
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Editar Chapa", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Seleccionar nueva imagen")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val uri = createImageUri(context)
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Tomar foto con cámara")
            }

            val imagenMostrada = nuevaImagenUri?.let { uri ->
                rememberAsyncImagePainter(uri)
            } ?: rememberAsyncImagePainter(File(chapa.imagePath))

            val scale = remember { mutableStateOf(1f) }
            val imageOffset = remember { mutableStateOf(Offset.Zero) }
            val frameSizeDp = 300.dp // Tamaño del marco cuadrado
            val density = LocalDensity.current
            val frameSizePx = with(density) { frameSizeDp.toPx() }

            Box(
                modifier = Modifier
                    .size(frameSizeDp)
                    .clip(RectangleShape) // Recorte cuadrado
                    .clipToBounds()       // Nada sale del marco
                    .background(Color.Black) // Opcional: para ver claramente el marco
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale.value *= zoom
                            scale.value = scale.value.coerceIn(0.5f, 3f)
                            imageOffset.value = Offset(
                                imageOffset.value.x + pan.x,
                                imageOffset.value.y + pan.y
                            )
                        }
                    }
            ) {
                Image(
                    painter = imagenMostrada,
                    contentDescription = "Imagen seleccionada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = imageOffset.value.x,
                            translationY = imageOffset.value.y
                        )
                        .offset {
                            val imageWidth = 512f
                            val imageHeight = 512f
                            val scaledWidth = imageWidth * scale.value
                            val scaledHeight = imageHeight * scale.value

                            val maxX = ((scaledWidth - frameSizePx) / 2).coerceAtLeast(0f)
                            val maxY = ((scaledHeight - frameSizePx) / 2).coerceAtLeast(0f)

                            val limitedX = imageOffset.value.x.coerceIn(-maxX, maxX)
                            val limitedY = imageOffset.value.y.coerceIn(-maxY, maxY)

                            IntOffset(limitedX.toInt(), limitedY.toInt())
                        }
                        .fillMaxSize()
                )

                OverlayCuadradoConGuiaCircular(
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(onClick = {
                    val finalImagePath = nuevaImagenUri?.let { uri ->
                        // Copia la nueva imagen al almacenamiento interno
                        ChapaViewModel.copyImageToInternalStorage(context, uri)
                    } ?: chapa.imagePath

                    val actualizada = chapa.copy(
                        nombre = nombre,
                        descripcion = descripcion,
                        imagePath = finalImagePath
                    )
                    // Llamas al onSave (esto se encarga de hacer update y resetear chapaAEditar)
                    onSave(actualizada)

                    // Opcional: limpiar campos si no dependes del estado externo
                    nombre = ""
                    descripcion = ""
                    nuevaImagenUri = null

                    // Navegación de vuelta
                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }) {
                    Text("Guardar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        }
    }

    private fun createImageUri(context: Context): Uri {
        val imageFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "chapa_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    }

    @Composable
    fun AddChapaScreen(viewModel: ChapaViewModel, navController: NavHostController) {
        val chapas by viewModel.allChapas.observeAsState(emptyList())

        var nombre by remember { mutableStateOf(TextFieldValue("")) }
        var descripcion by remember { mutableStateOf(TextFieldValue("")) }
        val context = LocalContext.current
        val imageUriState = remember { mutableStateOf<Uri?>(null) }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri -> imageUriState.value = uri }
        )
        val imageUri: Uri? = imageUriState.value
        val imageBitmap = remember(imageUri) {
            imageUri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                BitmapFactory.decodeStream(inputStream)
            }
        }
        var chapaAEditar by remember { mutableStateOf<Chapa?>(null) }

        val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    val uri = cameraImageUri.value
                    uri?.let {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        val squareBitmap = cropCenterSquare(originalBitmap)
                        val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                        val file = File(context.cacheDir, "chapa_camera_temp.jpg")
                        val outputStream = FileOutputStream(file)
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()

                        imageUriState.value = file.toUri()
                    }
                }
            }

        val galleryLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    val squareBitmap = cropCenterSquare(originalBitmap)
                    val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                    val file = File(context.cacheDir, "chapa_gallery_temp.jpg")
                    val outputStream = FileOutputStream(file)
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    imageUriState.value = file.toUri()
                }
            }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    val uri = createImageUri(context)
                    cameraImageUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val frameSizeDp = 300.dp // o el tamaño real de tu marco cuadrado
        val density = LocalDensity.current
        val frameSizePx = with(density) { frameSizeDp.toPx() }


        Column(modifier = Modifier.padding(16.dp)) {
            if (chapaAEditar != null) {
                EditChapaScreen(
                    chapa = chapaAEditar!!,
                    onSave = { chapaActualizada ->
                        viewModel.updateChapa(chapaActualizada)
                        chapaAEditar = null
                    },
                    onCancel = {
                        chapaAEditar = null
                    },
                    navController = navController
                )
                return // Evita que se renderice el resto
            }

            Text("Nueva Chapa", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
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
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Cámara")
                }
            }
            val scale = remember { mutableStateOf(1f) }
            val imageOffset = remember { mutableStateOf(Offset.Zero) }
            imageUriState.value?.let { uri ->
                Box(
                    modifier = Modifier
                        .size(frameSizeDp) // El tamaño del marco cuadrado
                        .padding(8.dp)
                        .clip(RectangleShape) // Recorta a forma cuadrada
                        .background(Color.Black) // Solo para ver el fondo del área visible (puedes quitarlo)
                        .clipToBounds() // Forza que nada sobresalga
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale.value *= zoom
                                scale.value = scale.value.coerceIn(0.5f, 3f)
                                imageOffset.value = Offset(imageOffset.value.x + pan.x, imageOffset.value.y + pan.y)
                            }
                        }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Imagen seleccionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale.value,
                                scaleY = scale.value,
                                translationX = imageOffset.value.x,
                                translationY = imageOffset.value.y
                            )
                            .offset {
                                val scaledWidth = (imageBitmap?.width ?: 0) * scale.value
                                val scaledHeight = (imageBitmap?.height ?: 0) * scale.value

                                val maxX = ((scaledWidth - frameSizePx) / 2).coerceAtLeast(0f)
                                val maxY = ((scaledHeight - frameSizePx) / 2).coerceAtLeast(0f)

                                val limitedX = imageOffset.value.x.coerceIn(-maxX, maxX)
                                val limitedY = imageOffset.value.y.coerceIn(-maxY, maxY)

                                IntOffset(limitedX.toInt(), limitedY.toInt())
                            }
                            .fillMaxSize()
                    )

                    OverlayCuadradoConGuiaCircular(
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val originalBitmap = imageBitmap
                    if (originalBitmap != null) {
                        val finalBitmap = recortarImagenVisible(
                            originalBitmap,
                            imageOffset.value,
                            scale.value,
                            frameSizePx
                        )

                        // Guardar a archivo temporal
                        val file = File(context.cacheDir, "chapa_final_recortada.jpg")
                        val outputStream = FileOutputStream(file)
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()

                        val finalUri = file.toUri()

                        // Pasar el URI final
                        viewModel.insertChapa(context, nombre.text, descripcion.text, finalUri)
                    }
                    //viewModel.insertChapa(context, nombre.text, descripcion.text, imageUri)

                    // Limpiar campos
                    nombre = TextFieldValue("")
                    descripcion = TextFieldValue("")
                    imageUriState.value = null

                    // Volver a la lista
                    navController.navigate(Screen.Lista.route) {
                        popUpTo(Screen.Lista.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Añadir Chapa")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    fun cropCenterSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
    }

    @Composable
    fun OverlayCuadradoConGuiaCircular(modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Fondo semitransparente
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Círculo guía (transparente)
            val circleRadius = size.minDimension / 2.2f
            /*drawCircle(
                color = Color.Transparent,
                radius = circleRadius,
                center = Offset(canvasWidth / 2, canvasHeight / 2),
                blendMode = BlendMode.Clear
            )

            // Borde blanco del círculo
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = circleRadius,
                center = Offset(canvasWidth / 2, canvasHeight / 2),
                style = Stroke(width = 4f)
            )*/
            drawCircle(
                color = Color.White,
                radius = circleRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx()) // Solo borde
            )
        }
    }

    fun recortarImagenVisible(
        bitmap: Bitmap,
        offset: Offset,
        scale: Float,
        frameSizePx: Float
    ): Bitmap {
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )

        val centerX = scaledBitmap.width / 2 - offset.x
        val centerY = scaledBitmap.height / 2 - offset.y

        val left = (centerX - frameSizePx / 2).coerceIn(0f, scaledBitmap.width - frameSizePx)
        val top = (centerY - frameSizePx / 2).coerceIn(0f, scaledBitmap.height - frameSizePx)

        return Bitmap.createBitmap(
            scaledBitmap,
            left.toInt(),
            top.toInt(),
            frameSizePx.toInt(),
            frameSizePx.toInt()
        )
    }

}
