// ChapaViewModel.kt
package com.tuempresa.chapacollectionapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.repository.ChapaRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ChapaViewModel(private val repository: ChapaRepository) : ViewModel() {

    private val _allChapas = MutableLiveData<List<Chapa>>()
    val allChapas: LiveData<List<Chapa>> get() = _allChapas

    init {
        loadChapas()
    }

    //esto puede hacer que observes dos veces la misma fuente si también estás usando repository.allChapas.asLiveData()
    //usando asLiveData() para observar directamente los cambios desde la base de datos, ya no necesitas una función loadChapas() para cargar manualmente los datos.
    fun loadChapas() {
        viewModelScope.launch {
            repository.getAllChapas().collect { lista ->
                _allChapas.postValue(lista)
            }
        }
    }

    fun insertChapa(context: Context, name: String, pais: String, imageUri: Uri?) {
        if(imageUri != null){
            val imagePath = imageUri?.let { copyImageToInternalStorage(context, it) }
            val chapa = Chapa(nombre = name, pais = pais, imagePath = imagePath)
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
}
