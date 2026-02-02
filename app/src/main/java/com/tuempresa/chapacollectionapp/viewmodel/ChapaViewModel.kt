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
import kotlin.text.toFloat
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChapaViewModel(private val repository: ChapaRepository) : ViewModel() {

    private val _allChapas = MutableLiveData<List<Chapa>>()
    val allChapas: LiveData<List<Chapa>> get() = _allChapas
    // En tu ChapaViewModel.kt
    var resultadosBusqueda by mutableStateOf<List<Chapa>>(emptyList())
        private set

    var estaBuscando by mutableStateOf(false)
        private set

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



    fun buscarCoincidencias(bitmapReferencia: Bitmap?, contexto: Context) {
        if (bitmapReferencia == null) {
            Log.e("BUSQUEDA", "El bitmap de referencia es NULO")
            return
        }

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
                    .filter { it.second > 20f } // BAJA EL UMBRAL AL 20% TEMPORALMENTE para ver si sale algo
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

    // Cambia esto en tu ChapaViewModel.kt
    private fun calcularSimilitud(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        // 1. Redimensionamos ambas para normalizar
        val b1 = Bitmap.createScaledBitmap(bitmap1, 64, 64, true)
        val b2 = Bitmap.createScaledBitmap(bitmap2, 64, 64, true)

        val hist1 = calcularHistograma(b1)
        val hist2 = calcularHistograma(b2)

        var similitud = 0f
        // Comparamos los 32 niveles de colores de ambos histogramas
        for (i in hist1.indices) {
            similitud += Math.min(hist1[i], hist2[i])
        }

        return similitud * 100f
    }

    private fun calcularHistograma(bitmap: Bitmap): FloatArray {
        val histograma = FloatArray(32 * 3) // 32 niveles para R, G y B
        val width = bitmap.width
        val height = bitmap.height
        val totalPixeles = (width * height).toFloat()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)

                // Reducimos el rango de colores de 256 a 32 para agrupar tonos similares
                val r = (android.graphics.Color.red(pixel) / 8)
                val g = (android.graphics.Color.green(pixel) / 8)
                val b = (android.graphics.Color.blue(pixel) / 8)

                histograma[r]++
                histograma[32 + g]++
                histograma[64 + b]++
            }
        }

        // Normalizamos el histograma (para que no importe si una foto es más grande que otra)
        for (i in histograma.indices) {
            histograma[i] /= (totalPixeles * 3)
        }

        return histograma
    }
}
