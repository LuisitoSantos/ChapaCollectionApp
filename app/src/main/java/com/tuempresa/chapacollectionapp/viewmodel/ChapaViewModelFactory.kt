package com.tuempresa.chapacollectionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuempresa.chapacollectionapp.components.SupabaseService

class ChapaViewModelFactory(
    private val supabaseService: SupabaseService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChapaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Ahora inyectamos el servicio de Supabase en lugar del de Firebase
            return ChapaViewModel(supabaseService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
