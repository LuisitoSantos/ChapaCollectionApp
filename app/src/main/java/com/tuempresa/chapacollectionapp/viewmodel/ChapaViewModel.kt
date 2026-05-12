// ChapaViewModel.kt
package com.tuempresa.chapacollectionapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import com.tuempresa.chapacollectionapp.data.Chapa
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tuempresa.chapacollectionapp.utils.GeoRepository
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuempresa.chapacollectionapp.components.SupabaseService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll


class ChapaViewModel(
    // Inyectamos el servicio de Supabase.
    // Si no usas Inyección de Dependencias (Hilt), lo inicializamos por defecto:
    private val supabaseService: SupabaseService = SupabaseService()
) : ViewModel() {

    private var isUpdating = false

    // El repositorio de coordenadas se queda como propiedad de la clase
    private var geoRepository: GeoRepository? = null

    // Esta función la sigues necesitando para inicializar el GPS/Mapas con el contexto de la App
    fun inicializarGeo(context: android.content.Context) {
        if (geoRepository == null) {
            geoRepository = GeoRepository(context)
        }
    }

    // --- ESTADOS DE LA UI ---
    private val _allChapas = MutableLiveData<List<Chapa>>(emptyList())
    val allChapas: LiveData<List<Chapa>> get() = _allChapas

    // Para Compose
    private val _chapasSupabase = mutableStateOf<List<Chapa>>(emptyList())
    val chapasSupabase: State<List<Chapa>> = _chapasSupabase

    var resultadosBusqueda by mutableStateOf<List<Chapa>>(emptyList())
    var estaBuscando by mutableStateOf(false)
    var vistaCuadricula by mutableStateOf(false)
        private set

    init {
        cargarChapasDeSupabase()
    }

    // Obtener listas únicas de la base de datos para sugerencias
    //val sugerenciasDonantes: LiveData<List<String>> = repository.getUniqueDonantes().asLiveData()

    // Sustituye las líneas de sugerencias por estas:
    val sugerenciasPaises: LiveData<List<String>> = allChapas.map { lista ->
        lista.map { it.pais }.distinct().sorted()
    }

    val sugerenciasCiudades: LiveData<List<String>> = allChapas.map { lista ->
        lista.mapNotNull { it.ciudad }.distinct().sorted()
    }

    val sugerenciasDonantes: LiveData<List<String>> = allChapas.map { lista ->
        lista.mapNotNull { it.donante }.distinct().sorted()
    }



    // Creamos una variable para saber si ya hemos cargado la preferencia
    private var preferenciaCargada = false


    fun getChapaById(id: String): LiveData<Chapa?> {
        val result = MutableLiveData<Chapa?>()
        // Convertimos el id (String) a Long para poder comparar
        val idLong = id.toLongOrNull()

        // Buscamos en la lista que ya tenemos en memoria
        val chapa = allChapas.value?.find { it.id == idLong }

        result.value = chapa
        return result
    }

    fun buscarCoincidencias(bitmapReferencia: Bitmap?, contexto: Context, umbral: Float) {
        if (bitmapReferencia == null) return
        resultadosBusqueda = emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            estaBuscando = true
            try {
                // 1. Obtenemos todas las chapas de Supabase (ya lo hace loadChapas, pero aquí nos aseguramos)
                val todasLasChapas = allChapas.value ?: emptyList()
                val imageLoader = ImageLoader(contexto)

                // 2. Procesamos en paralelo para ir rápido
                val encontradas = todasLasChapas.chunked(5).flatMap { grupo ->
                    grupo.map { chapa ->
                        async {
                            val bitmapChapa = downloadBitmap(contexto, imageLoader, chapa.imagePath)
                            val porcentaje = if (bitmapChapa != null) {
                                calcularSimilitud(bitmapReferencia, bitmapChapa)
                            } else 0f
                            Pair(chapa, porcentaje)
                        }
                    }.awaitAll()
                }
                    .filter { it.second >= umbral }
                    .sortedByDescending { it.second }
                    .map { it.first }

                withContext(Dispatchers.Main) {
                    resultadosBusqueda = encontradas
                    estaBuscando = false
                }
            } catch (e: Exception) {
                Log.e("BUSQUEDA", "Error: ${e.message}")
                withContext(Dispatchers.Main) { estaBuscando = false }
            }
        }
    }

    // Función auxiliar para descargar la imagen a memoria sin guardar archivo
    private suspend fun downloadBitmap(context: Context, loader: ImageLoader, url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Necesario para poder manipular los píxeles después
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun calcularSimilitud(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        // Redimensionar un poco más grande ayuda a capturar mejores transiciones de color
        val b1 = Bitmap.createScaledBitmap(bitmap1, 100, 100, true)
        val b2 = Bitmap.createScaledBitmap(bitmap2, 100, 100, true)

        val hist1 = calcularHistograma(b1)
        val hist2 = calcularHistograma(b2)

        var similitud = 0f
        for (i in hist1.indices) {
            // Intersección de histogramas
            similitud += Math.min(hist1[i], hist2[i])
        }

        // Como comparamos dos propiedades (Hue y Saturation), el máximo teórico es 2.0
        // Lo normalizamos a base 100
        return (similitud / 2f) * 100f
    }

    private fun calcularHistograma(bitmap: Bitmap): FloatArray {
        // Usaremos 30 divisiones para el Matiz (Hue), que es lo más importante
        val hBins = 30
        val sBins = 10
        val histograma = FloatArray(hBins + sBins)

        val width = bitmap.width
        val height = bitmap.height
        val totalPixeles = (width * height).toFloat()

        val hsv = FloatArray(3)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)

                // Convertimos el píxel de RGB a HSV
                android.graphics.Color.colorToHSV(pixel, hsv)

                val h = hsv[0] // 0 a 360 (Color)
                val s = hsv[1] // 0 a 1 (Saturación)
                val v = hsv[2] // 0 a 1 (Brillo/Sombras)

                // Ignoramos píxeles demasiado oscuros (sombras extremas) o muy blancos (reflejos)
                if (v > 0.15f && v < 0.95f) {
                    // Clasificamos el matiz (Hue)
                    val hIndex = ((h / 360f) * (hBins - 1)).toInt()
                    histograma[hIndex]++

                    // Clasificamos la saturación
                    val sIndex = (s * (sBins - 1)).toInt()
                    histograma[hBins + sIndex]++
                }
            }
        }

        // Normalizamos
        for (i in histograma.indices) {
            histograma[i] /= totalPixeles
        }

        return histograma
    }

    fun cargarPreferenciaVista(context: Context) {
        if (!preferenciaCargada) {
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            vistaCuadricula = prefs.getBoolean("is_grid", false)
            preferenciaCargada = true
        }
    }

    fun setVistaCuadricula(context: Context, activa: Boolean) {
        vistaCuadricula = activa
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_grid", activa).apply()
    }

    fun fetchChapas() {
        viewModelScope.launch {
            val lista = supabaseService.getChapas()
            _chapasSupabase.value = lista
            Log.d("ViewModel", "Chapas cargadas: ${lista.size}")
        }
    }

    fun cargarChapasDeSupabase() {
        viewModelScope.launch {
            try {
                val lista = supabaseService.getChapas()
                // Actualizamos el estado de Compose (para la ChapaListScreen)
                _chapasSupabase.value = lista
                // Actualizamos el LiveData (para que los buscadores y sugerencias sigan funcionando)
                _allChapas.postValue(lista)

                Log.d("Supabase", "Chapas cargadas: ${lista.size}")
            } catch (e: Exception) {
                Log.e("Supabase", "Error al cargar lista: ${e.message}")
            }
        }
    }

    fun saveInSupabase(context: Context, chapa: Chapa, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                // Buscamos coordenadas antes de enviar a Supabase para no perder esa función
                val coords = geoRepository?.getCoordinates(chapa.pais, chapa.ciudad)
                val chapaConCoords = chapa.copy(
                    latitud = coords?.first ?: chapa.latitud,
                    longitud = coords?.second ?: chapa.longitud
                )

                supabaseService.saveChapa(context, chapaConCoords, imageUri)

                // IMPORTANTE: Refrescamos la lista para que la nueva chapa aparezca al volver
                cargarChapasDeSupabase()
            } catch (e: Exception) {
                Log.e("Supabase", "Error al guardar: ${e.message}")
            }
        }
    }

    fun deleteChapaSupabase(chapa: Chapa) {
        viewModelScope.launch {
            try {
                // 1. Borrar la imagen del Storage si existe
                if (!chapa.imagePath.isNullOrEmpty()) {
                    supabaseService.deleteImageFromStorage(chapa.imagePath)
                }

                // 2. Borrar el registro de la base de datos
                supabaseService.deleteChapa(chapa)

                cargarChapasDeSupabase()
                Log.d("Supabase", "Chapa e imagen eliminadas: ${chapa.nombre}")
            } catch (e: Exception) {
                Log.e("Supabase", "Error al eliminar: ${e.message}")
            }
        }
    }

/*
    fun updateChapaEnSupabase(context: Context, chapaOriginal: Chapa, chapaEditada: Chapa, nuevaImageUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Recalcular coordenadas por si cambió el país o ciudad
                val coords = geoRepository?.getCoordinates(chapaEditada.pais, chapaEditada.ciudad)

                // 2. Creamos el objeto con las nuevas coordenadas (la imagen la gestiona el servicio)
                val chapaConCoords = chapaEditada.copy(
                    latitud = coords?.first ?: chapaOriginal.latitud,
                    longitud = coords?.second ?: chapaOriginal.longitud
                )

                // 3. LLAMADA AL SERVICIO (Pasando los 3 parámetros que pide tu SupabaseService)
                supabaseService.updateChapa(
                    context = context,
                    chapa = chapaConCoords,
                    nuevaImageUri = nuevaImageUri
                )

                // 4. Refrescar la lista
                cargarChapasDeSupabase()

            } catch (e: Exception) {
                Log.e("Supabase", "Error al actualizar en ViewModel: ${e.message}")
            }
        }
    }
 */

/*
    //ESTE ES EL QUE MAS O MENOS FUNCIONA
    suspend fun updateChapaEnSupabase(context: Context, chapaOriginal: Chapa, chapaEditada: Chapa, nuevaImageUri: Uri?) {
        if (isUpdating) return // Si ya está actualizando, ignoramos la segunda llamada
        isUpdating = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Obtener coordenadas si cambiaron
                val coords = geoRepository?.getCoordinates(chapaEditada.pais, chapaEditada.ciudad)

                // 2. Llamar al servicio (que ahora maneja el borrado y subida)
                supabaseService.updateChapa(
                    context = context,
                    chapa = chapaEditada.copy(
                        latitud = coords?.first ?: chapaOriginal.latitud,
                        longitud = coords?.second ?: chapaOriginal.longitud
                    ),
                    nuevaImageUri = nuevaImageUri
                )

                cargarChapasDeSupabase()
            } catch (e: Exception) {
                Log.e("Supabase", "Error: ${e.message}")
            } finally {
                isUpdating = false // Liberamos el bloqueo
            }
        }
    }
    */

    suspend fun updateChapaEnSupabase(context: Context, chapaOriginal: Chapa, chapaEditada: Chapa, nuevaImageUri: Uri?) {
        // 1. Bloqueo de seguridad en el hilo principal
        if (isUpdating) return
        isUpdating = true

        try {
            // 2. Ejecutar la lógica pesada en IO
            withContext(Dispatchers.IO) {
                // Obtener coordenadas si cambiaron
                val coords = geoRepository?.getCoordinates(chapaEditada.pais, chapaEditada.ciudad)

                // Preparamos el objeto asegurando que pasamos el imagePath original
                // para que el Service sepa qué borrar.
                val chapaParaActualizar = chapaEditada.copy(
                    imagePath = chapaOriginal.imagePath,
                    latitud = coords?.first ?: chapaOriginal.latitud,
                    longitud = coords?.second ?: chapaOriginal.longitud
                )

                // LLAMADA ÚNICA AL SERVICIO
                supabaseService.updateChapa(
                    context = context,
                    chapa = chapaParaActualizar,
                    nuevaImageUri = nuevaImageUri
                )

                // Refrescamos datos
                val lista = supabaseService.getChapas()

                withContext(Dispatchers.Main) {
                    _chapasSupabase.value = lista
                    _allChapas.value = lista
                }
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error: ${e.message}")
        } finally {
            // 3. Liberar el bloqueo al final
            isUpdating = false
        }
    }


/*
    suspend fun updateChapaEnSupabase(context: Context, chapaOriginal: Chapa, chapaEditada: Chapa, nuevaImageUri: Uri?) {
        withContext(Dispatchers.IO) {
            try {
                var urlFinal = chapaOriginal.imagePath

                if (nuevaImageUri != null) {
                    // SI HAY IMAGEN NUEVA:
                    // Primero borramos lo que haya actualmente en Storage
                    chapaOriginal.imagePath?.let { oldUrl ->
                        if (oldUrl.contains("http")) { // Solo borrar si es una URL de internet
                            supabaseService.deleteImageFromStorage(oldUrl)
                        }
                    }

                    // Subimos la nueva (da igual si viene de cámara o galería)
                    val newUrl = supabaseService.uploadImage(context, nuevaImageUri)
                    if (newUrl != null) {
                        urlFinal = newUrl
                    }
                }

                // Actualizar la tabla en Supabase
                val chapaConNuevaImagen = chapaEditada.copy(imagePath = urlFinal)
                supabaseService.updateChapa(context, chapaConNuevaImagen, nuevaImageUri)

                // RECARGA CRÍTICA:
                // Después de actualizar, descargamos la lista de nuevo para que
                // la próxima edición tenga la URL fresca.
                cargarChapasDeSupabase()

            } catch (e: Exception) {
                Log.e("Supabase", "Error: ${e.message}")
            }
        }
    } */

}
