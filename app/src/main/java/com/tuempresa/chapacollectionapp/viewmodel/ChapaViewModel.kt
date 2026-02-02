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
import com.tuempresa.chapacollectionapp.repository.ChapaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChapaViewModel(private val repository: ChapaRepository) : ViewModel() {

    private val _allChapas = MutableLiveData<List<Chapa>>()
    val allChapas: LiveData<List<Chapa>> get() = _allChapas
    // En tu ChapaViewModel.kt
    var resultadosBusqueda by mutableStateOf<List<Chapa>>(emptyList())

    var estaBuscando by mutableStateOf(false)

    init {
        loadChapas()
    }

    fun loadChapas() {
        viewModelScope.launch {
            repository.getAllChapas().collect { lista ->
                _allChapas.postValue(lista)
            }
        }
    }

    fun insertChapa(
        context: Context,
        name: String,
        pais: String,
        imageUri: Uri?,
        anio: Int? = null,
        colorPrimario: String = "",
        colorSecundario1: String? = null,
        colorSecundario2: String? = null,
        estadoForma: String? = null,
        estadoRayones: String? = null,
        estadoMarcas: String? = null,
        estadoOxido: String? = null,
        estadoPercent: Int? = null
    ) {
        if(imageUri != null){
            val imagePath = copyImageToInternalStorage(context, imageUri)
            val chapa = Chapa(
                nombre = name,
                pais = pais,
                imagePath = imagePath,
                anio = anio,
                colorPrimario = colorPrimario,
                colorSecundario1 = colorSecundario1,
                colorSecundario2 = colorSecundario2,
                estadoForma = estadoForma,
                estadoRayones = estadoRayones,
                estadoMarcas = estadoMarcas,
                estadoOxido = estadoOxido,
                estadoPercent = estadoPercent
            )
            viewModelScope.launch {
                repository.insert(chapa)
                loadChapas() // Actualiza la lista
            }
        }
    }

    fun deleteChapa(chapa: Chapa) {
        viewModelScope.launch {
            repository.delete(chapa)
            //loadChapas()
        }
    }

    fun updateChapa(chapa: Chapa) {
        viewModelScope.launch {
            repository.update(chapa)
            loadChapas()
        }
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
}
