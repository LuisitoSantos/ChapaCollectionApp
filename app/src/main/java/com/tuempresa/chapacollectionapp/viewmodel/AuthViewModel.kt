package com.tuempresa.chapacollectionapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email // IMPORTANTE: Este es el correcto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class AuthViewModel(private val client: SupabaseClient) : ViewModel() {

    // Estado para controlar si el usuario está logueado o no
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Función para iniciar sesión con Email y Password
    fun login(email: String, pass: String) {
        viewModelScope.launch { // Esto corre en un hilo seguro
            _authState.value = AuthState.Loading
            try {
                // Log para depurar
                println("Intentando login para: $email")

                client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }

                println("Login exitoso")
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // CAPTURA EL ERROR REAL
                e.printStackTrace() // Esto hará que el error aparezca en el Logcat de Android Studio
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Función para registrarse (opcional)
    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    // Cerrar sesión
    fun signOut() {
        viewModelScope.launch {
            try {
                client.auth.signOut() // Cierra sesión en Supabase
            } catch (e: Exception) {
                Log.e("Auth", "Error al cerrar sesión", e)
            } finally {
                // REPETO IMPORTANTE: Devolvemos el estado al inicio (Idle o inicial)
                // para que la pantalla de Login no piense que sigue logueado o cargando
                _authState.value = AuthState.Idle
            }
        }
    }

    // Obtener el usuario actual
    //val currentUser = client.auth.currentSessionOrNull()?.user
    val currentUser get() = client.auth.currentSessionOrNull()?.user
}

// Clase para manejar los estados de la pantalla de Login
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}



class AuthViewModelFactory(private val client: SupabaseClient) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
