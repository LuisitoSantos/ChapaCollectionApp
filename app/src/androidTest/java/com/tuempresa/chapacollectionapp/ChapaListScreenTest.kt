/*
package com.tuempresa.chapacollectionapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.chapacollectionapp.ui.screens.SearchChapaScreen
// IMPORTANTE: Asegúrate de importar tu ViewModel y la Factory si la usas
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.lifecycle.viewmodel.compose.viewModel

@RunWith(AndroidJUnit4::class)
class ChapaListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listaSeMuestraAlInicio() {
        startScreen()
        // Verificamos que al menos la barra de búsqueda exista
        composeTestRule.onNodeWithTag("barra_busqueda").assertExists()
    }

    @Test
    fun verificarBotonCambioVistaExiste() {
        startScreen()
        composeTestRule.onNodeWithTag("boton_cambio_vista").assertExists()
    }

    // Helper para configurar la pantalla
    private fun startScreen() {
        composeTestRule.setContent {
            val navController = rememberNavController()

            // 1. Obtenemos el viewModel dentro del Composable del test
            // Si tu ViewModel necesita parámetros, aquí es donde fallará si no los pasas.
            // Para que compile, asegúrate de que SearchChapaScreen sea accesible.
            SearchChapaScreen(
                viewModel = viewModel(), // Esto crea una instancia básica del VM
                navController = navController
            )
        }
    }
}
*/
