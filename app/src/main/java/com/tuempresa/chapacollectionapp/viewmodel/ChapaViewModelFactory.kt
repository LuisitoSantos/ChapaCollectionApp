package com.tuempresa.chapacollectionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuempresa.chapacollectionapp.components.FirebaseService

// Para Room, el factory se ve así, inyectando el repositorio:
/*
class ChapaViewModelFactory(private val repository: FirebaseService) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapaViewModel::class.java)) {
            return ChapaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
 */

//Para Firebase, el factory se ve así, inyectando el servicio en lugar del repositorio de Room:
class ChapaViewModelFactory(
    private val firebaseService: FirebaseService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChapaViewModel(firebaseService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
