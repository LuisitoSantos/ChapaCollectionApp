/*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.chapacollectionapp.ui.screens.SearchChapaScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchChapaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun startScreen() {
        composeTestRule.setContent {
            SearchChapaScreen(
                navController = rememberNavController(),
                // Aquí se asume que el ViewModel ya tiene datos o está mockeado
            )
        }
    }

    @Test
    fun verificarEstadoInicialVacio() {
        startScreen()

        // Verificar que la barra de búsqueda existe y está vacía
        composeTestRule.onNodeWithTag("barra_busqueda").assertTextContains("")
        //composeTestRule.onNodeWithTag("barra_busqueda").assertTextEquals("ESTO DEBERIA FALLAR")

        // Verificar que, si no hay búsqueda, quizás hay un texto de "Empieza a buscar"
        // composeTestRule.onNodeWithText("Introduce un nombre para buscar").assertIsDisplayed()
    }

    @Test
    fun escribirEnBusquedaYVerificarTexto() {
        startScreen()

        // Simular que el usuario escribe "Estrella"
        composeTestRule.onNodeWithTag("barra_busqueda").performTextInput("Estrella")

        // Verificar que el texto se muestra correctamente
        composeTestRule.onNodeWithTag("barra_busqueda").assertTextEquals("Estrella")
    }

    @Test
    fun borrarBusquedaLimpiaResultados() {
        startScreen()

        // Escribimos algo
        composeTestRule.onNodeWithTag("barra_busqueda").performTextInput("Coleccion")

        // Limpiamos el texto (usando el botón X si lo tienes o reemplazando por vacío)
        composeTestRule.onNodeWithTag("barra_busqueda").performTextReplacement("")

        // Verificar que vuelve a estar vacío
        composeTestRule.onNodeWithTag("barra_busqueda").assertTextEquals("")
    }

    @Test
    fun verificarQueAparecenResultados() {
        startScreen()

        // Buscamos un término que sepamos que existe en nuestros datos de prueba
        composeTestRule.onNodeWithTag("barra_busqueda").performTextInput("Chapa")

        // Verificamos que la lista de resultados se despliega
        // Nota: Esto asume que el ViewModel reacciona a la búsqueda
        composeTestRule.onNodeWithTag("lista_resultados").assertExists()
    }
}

 */