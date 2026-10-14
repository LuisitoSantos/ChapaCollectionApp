package com.tuempresa.chapacollectionapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email // IMPORTANTE: Este es el correcto
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.tuempresa.chapacollectionapp.data.PublicProfile
import io.github.jan.supabase.postgrest.from

class AuthViewModel(private val client: SupabaseClient) : ViewModel() {

    // Estado para controlar si el usuario está logueado o no
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        viewModelScope.launch {
            client.auth.sessionStatus.collect { status ->
                if (status !is SessionStatus.LoadingFromStorage && client.auth.currentSessionOrNull() != null) {
                    _authState.value = AuthState.Success
                }
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                client.auth.signInWith(Google)
                // El inicio termina cuando Supabase devuelve el deep link a la app.
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "No se pudo iniciar sesión con Google")
            }
        }
    }

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
                _authState.value = if (client.auth.currentSessionOrNull() != null) {
                    AuthState.Success
                } else {
                    AuthState.Error("Cuenta creada. Confirma tu correo y vuelve a iniciar sesión.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        val session = client.auth.currentSessionOrNull()
        return session != null && session.refreshToken != null
    }

    // Cerrar sesión
    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (_authState.value == AuthState.Loading) return@launch
            _authState.value = AuthState.Loading
            try {
                client.auth.signOut() // Cierra sesión en Supabase
            } catch (e: Exception) {
                Log.e("Auth", "Error al cerrar sesión", e)
            } finally {
                // REPETO IMPORTANTE: Devolvemos el estado al inicio (Idle o inicial)
                // para que la pantalla de Login no piense que sigue logueado o cargando
                _authState.value = AuthState.Idle
                onComplete()
            }
        }
    }

    suspend fun hasPublicProfile(): Boolean {
        val userId = client.auth.currentSessionOrNull()?.user?.id ?: return false
        return client.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeList<PublicProfile>().isNotEmpty()
    }

    fun createPublicProfile(username: String) {
        val normalized = username.trim().lowercase()
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                require(normalized.matches(Regex("^[a-z0-9_]{3,20}$"))) {
                    "Usa entre 3 y 20 caracteres: letras, números o guion bajo."
                }
                val userId = client.auth.currentSessionOrNull()?.user?.id
                    ?: throw IllegalStateException("La sesión ha caducado. Inicia sesión de nuevo.")
                val exists = client.from("profiles").select {
                    filter { eq("username", normalized) }
                }.decodeList<PublicProfile>().isNotEmpty()
                require(!exists) { "Ese nombre público ya está en uso." }
                client.from("profiles").insert(PublicProfile(id = userId, username = normalized))
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                val message = e.message.orEmpty()
                _profileState.value = ProfileState.Error(
                    if (message.contains("23505") || message.contains("duplicate", ignoreCase = true)) {
                        "Ese nombre público ya está en uso. Prueba con otro."
                    } else message.ifBlank { "No se pudo guardar el nombre público." }
                )
            }
        }
    }

    fun resetProfileState() { _profileState.value = ProfileState.Idle }

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

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
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
