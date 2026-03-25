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
//import com.tuempresa.chapacollectionapp.repository.ChapaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.asLiveData
import com.tuempresa.chapacollectionapp.components.FirebaseService
import com.tuempresa.chapacollectionapp.utils.GeoRepository
import com.google.android.gms.tasks.Tasks
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll


class ChapaViewModel(
    //private val repository: ChapaRepository,
    private val firebaseService: FirebaseService = FirebaseService()) : ViewModel() {
    // 1. Repositorio de la Base de Datos Room
    //private val repository: ChapaRepository

    // 2. Repositorio de Coordenadas (GeoNames)
    private var geoRepository: GeoRepository? = null

    fun inicializarGeo(context: android.content.Context) {
        if (geoRepository == null) {
            geoRepository = GeoRepository(context)
        }
    }

    private val _allChapas = MutableLiveData<List<Chapa>>()
    val allChapas: LiveData<List<Chapa>> get() = _allChapas
    // En tu ChapaViewModel.kt
    var resultadosBusqueda by mutableStateOf<List<Chapa>>(emptyList())

    var estaBuscando by mutableStateOf(false)

    var vistaCuadricula by mutableStateOf(false)
        private set

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

    init {
        loadChapas()
    }

    //Para Room
    /*
    fun loadChapas() {
        viewModelScope.launch {
            repository.getAllChapas().collect { lista ->
                _allChapas.postValue(lista)
            }
        }
    }*/

    //Para Firebase
    fun loadChapas() {
        viewModelScope.launch {
            // Ahora escuchamos el Flow que creamos en FirebaseService
            firebaseService.getChapasFlow().collect { lista ->
                _allChapas.postValue(lista)
            }
        }
    }

    //Para Room
    /*
    fun insertChapa(
        context: Context,
        name: String,
        pais: String,
        ciudad: String? = null,
        imageUri: Uri?,
        anio: Int? = null,
        colorPrimario: String = "",
        colorSecundario1: String? = null,
        colorSecundario2: String? = null,
        estadoForma: String? = null,
        estadoRayones: String? = null,
        estadoMarcas: String? = null,
        estadoOxido: String? = null,
        estadoPercent: Int? = null,
        procedencia: String? = null,
        metodoObtencion: String? = null,
        donante: String? = null,
        paisObtencion: String? = null,
        ciudadObtencion: String? = null
    ) {
        if(imageUri != null){
            // 1. Buscamos las coordenadas antes de insertar
            val coords = geoRepository?.getCoordinates(pais, ciudad)

            val imagePath = copyImageToInternalStorage(context, imageUri)
            val chapa = Chapa(
                nombre = name,
                pais = pais,
                ciudad = ciudad,
                imagePath = imagePath,
                anio = anio,
                colorPrimario = colorPrimario,
                colorSecundario1 = colorSecundario1,
                colorSecundario2 = colorSecundario2,
                estadoForma = estadoForma,
                estadoRayones = estadoRayones,
                estadoMarcas = estadoMarcas,
                estadoOxido = estadoOxido,
                estadoPercent = estadoPercent,
                latitud = coords?.first ?: 0.0,
                longitud = coords?.second ?: 0.0,
                procedencia = procedencia,
                metodoObtencion = metodoObtencion,
                donante = donante,
                paisObtencion = paisObtencion,
                ciudadObtencion = ciudadObtencion
            )
            viewModelScope.launch {
                repository.insert(chapa)
                loadChapas() // Actualiza la lista
            }
        }
    }
     */

    //Para Firebase
    fun insertChapa(
        context: Context,
        name: String,
        pais: String,
        ciudad: String? = null,
        imageUri: Uri?, // Esta es la URI de la galería/cámara
        anio: Int? = null,
        colorPrimario: String = "",
        colorSecundario1: String? = null,
        colorSecundario2: String? = null,
        estadoForma: String? = null,
        estadoRayones: String? = null,
        estadoMarcas: String? = null,
        estadoOxido: String? = null,
        estadoPercent: Int? = null,
        procedencia: String? = null,
        metodoObtencion: String? = null,
        donante: String? = null,
        paisObtencion: String? = null,
        ciudadObtencion: String? = null
    ) {
        viewModelScope.launch {
            // 1. Buscamos coordenadas
            val coords = geoRepository?.getCoordinates(pais, ciudad)

            // 2. Creamos el objeto Chapa inicial
            val nuevaChapa = Chapa(
                nombre = name,
                pais = pais,
                ciudad = ciudad,
                anio = anio,
                colorPrimario = colorPrimario,
                colorSecundario1 = colorSecundario1,
                colorSecundario2 = colorSecundario2,
                estadoForma = estadoForma,
                estadoRayones = estadoRayones,
                estadoMarcas = estadoMarcas,
                estadoOxido = estadoOxido,
                estadoPercent = estadoPercent,
                latitud = coords?.first ?: 0.0,
                longitud = coords?.second ?: 0.0,
                procedencia = procedencia,
                metodoObtencion = metodoObtencion,
                donante = donante,
                paisObtencion = paisObtencion,
                ciudadObtencion = ciudadObtencion
            )

            // 3. Guardamos en Firebase (el servicio subirá la imagen si existe)
            try {
                firebaseService.saveChapa(nuevaChapa, imageUri)
                // Opcional: Puedes seguir guardando en Room como respaldo
                // repository.insert(nuevaChapa)
            } catch (e: Exception) {
                Log.e("FIREBASE", "Error al insertar: ${e.message}")
            }
        }
    }

    //Para Room
    /*
    fun deleteChapa(chapa: Chapa) {
        viewModelScope.launch {
            repository.delete(chapa)
            //loadChapas()
        }
    }
     */

    //Para Firebase
    /*fun deleteChapa(chapa: Chapa) {
        viewModelScope.launch {
            // Borramos de Firebase usando el ID de la nube
            firebaseService.deleteChapa(chapa.firestoreId)
            // También de Room si lo sigues usando
            repository.delete(chapa)
        }
    }*/

    fun deleteChapa(chapa: Chapa) {
        viewModelScope.launch {
            // Borramos SOLO de Firebase
            firebaseService.deleteChapa(chapa.firestoreId)
            // No hace falta llamar a loadChapas() porque el SnapshotListener
            // de Firebase detectará el borrado y refrescará la lista solo.
        }
    }


    //Para Room
    /*
    fun updateChapa(chapa: Chapa) {
        viewModelScope.launch {
            // Buscamos coordenadas nuevas usando el GeoRepository
            val coords = geoRepository?.getCoordinates(chapa.pais, chapa.ciudad)

            // Creamos la chapa definitiva con las coordenadas encontradas
            val chapaFinal = chapa.copy(
                latitud = coords?.first ?: chapa.latitud,
                longitud = coords?.second ?: chapa.longitud
            )
            repository.update(chapaFinal)
            loadChapas()
        }
    }
     */

    //Para Firebase
    fun updateChapa(chapa: Chapa, nuevaImageUri: Uri? = null) {
        viewModelScope.launch {
            val coords = geoRepository?.getCoordinates(chapa.pais, chapa.ciudad)
            val chapaFinal = chapa.copy(
                latitud = coords?.first ?: chapa.latitud,
                longitud = coords?.second ?: chapa.longitud
            )

            // Actualizamos en Firebase
            firebaseService.saveChapa(chapaFinal, nuevaImageUri)
        }
    }

    //Para Room
    /*
    fun getChapaById(id: Int): LiveData<Chapa?> {
        return repository.getChapaById(id).asLiveData()
    }
     */

    //Para Firebase
    /*fun getChapaById(firestoreId: String): LiveData<Chapa?> {
        return repository.getChapaById(firestoreId).asLiveData()
    }
     */

    fun getChapaById(firestoreId: String): LiveData<Chapa?> {
        val result = MutableLiveData<Chapa?>()
        // Buscamos en la lista que ya tenemos en memoria
        val chapa = allChapas.value?.find { it.firestoreId == firestoreId }
        result.value = chapa
        return result
    }

    companion object {
        fun copyImageToInternalStorage(context: Context, uri: Uri): String {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "chapa_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file.absolutePath
        }
    }

    //Para Room
    /*
    fun buscarCoincidencias(bitmapReferencia: Bitmap?, contexto: Context, umbral: Float) {
        if (bitmapReferencia == null) {
            Log.e("BUSQUEDA", "El bitmap de referencia es NULO")
            return
        }
        resultadosBusqueda = emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            estaBuscando = true
            try {
                val todasLasChapas = repository.getAllChapas().first()
                Log.d("BUSQUEDA", "Total de chapas en BD: ${todasLasChapas.size}")

                val encontradas = todasLasChapas.map { chapa ->
                    val file = File(chapa.imagePath)
                    val bitmapChapa = if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null

                    if (bitmapChapa == null) {
                        Log.e("BUSQUEDA", "No se pudo cargar imagen de: ${chapa.nombre} en ruta: ${chapa.imagePath}")
                    }

                    val porcentaje = if (bitmapChapa != null) {
                        calcularSimilitud(bitmapReferencia, bitmapChapa)
                    } else 0f

                    Log.d("BUSQUEDA", "Comparando con: ${chapa.nombre} - Similitud: $porcentaje%")
                    Pair(chapa, porcentaje)
                }
                    .filter { it.second >= umbral } // BAJA EL UMBRAL AL 20% TEMPORALMENTE para ver si sale algo
                    .sortedByDescending { it.second }
                    .map { it.first }

                withContext(Dispatchers.Main) {
                    resultadosBusqueda = encontradas
                    estaBuscando = false
                    Log.d("BUSQUEDA", "Busqueda finalizada. Encontradas: ${encontradas.size}")
                }
            } catch (e: Exception) {
                Log.e("BUSQUEDA", "Error critico: ${e.message}")
                withContext(Dispatchers.Main) { estaBuscando = false }
            }
        }
    }*/

    //Para Firebase
    fun buscarCoincidencias(bitmapReferencia: Bitmap?, contexto: Context, umbral: Float) {
        if (bitmapReferencia == null) return
        resultadosBusqueda = emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            estaBuscando = true
            try {
                // 1. Obtenemos todas las chapas de Firebase (ya lo hace loadChapas, pero aquí nos aseguramos)
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
}
