// ChapaViewModel.kt
package com.tuempresa.chapacollectionapp.viewmodel

import android.content.Context
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

class ChapaViewModel(private val repository: ChapaRepository) : ViewModel() {

    private val _allChapas = MutableLiveData<List<Chapa>>()
    val allChapas: LiveData<List<Chapa>> get() = _allChapas
    // En tu ChapaViewModel.kt
    var resultadosBusqueda by mutableStateOf<List<Chapa>>(emptyList())
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



    fun buscarCoincidencias(uriReferencia: Uri) {
        // Aquí iría la lógica algorítmica.
        // Por ahora, simularemos que filtramos las chapas que tienen una imagen válida
        // para demostrar cómo se muestran los resultados.
        viewModelScope.launch {
            val todas = repository.getAllChapas().first() // Asumiendo que usas Flow
            resultadosBusqueda = todas.take(3) // Simulamos que encuentra las 3 primeras
        }
    }
}
