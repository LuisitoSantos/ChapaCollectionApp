/*
 androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.chapacollectionapp.ui.screens.AddChapaScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddChapaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Helper para cargar la pantalla
    private fun startScreen() {
        composeTestRule.setContent {
            // Sustituye por el nombre real de tu Composable y sus parámetros
            AddChapaScreen(
                navController = rememberNavController(),
                // Aquí podrías pasar un ViewModel mock si lo necesitas
            )
        }
    }

    @Test
    fun verificarCamposVaciosAlInicio() {
        startScreen()

        // Verificar que los campos existen y están vacíos
        composeTestRule.onNodeWithTag("campo_nombre").assertTextContains("")
        composeTestRule.onNodeWithTag("campo_anio").assertTextContains("")

        // El botón de guardar debería estar deshabilitado si no hay datos (si tienes esa lógica)
        // composeTestRule.onNodeWithTag("boton_guardar").assertIsNotEnabled()
    }

    @Test
    fun escribirDatosYVerificarTexto() {
        startScreen()

        // Escribir nombre
        composeTestRule.onNodeWithTag("campo_nombre").performTextInput("Colección Especial")

        // Escribir año
        composeTestRule.onNodeWithTag("campo_anio").performTextInput("2024")

        // Verificar que el texto se ha introducido correctamente
        composeTestRule.onNodeWithTag("campo_nombre").assertTextEquals("Colección Especial")
        composeTestRule.onNodeWithTag("campo_anio").assertTextEquals("2024")
    }

    @Test
    fun mostrarErrorSiFaltanCamposObligatorios() {
        startScreen()

        // Intentar guardar sin poner nada
        composeTestRule.onNodeWithTag("boton_guardar").performClick()

        // Verificar si aparece algún mensaje de error (Snackbar o Texto)
        // Esto depende de cómo manejes los errores. Si usas un texto de error:
        composeTestRule.onNodeWithText("El nombre es obligatorio").assertIsDisplayed()
    }

    @Test
    fun verificarInteraccionConImagen() {
        startScreen()

        // Pulsar el botón de seleccionar imagen/cámara
        composeTestRule.onNodeWithTag("boton_camara").performClick()

        // Aquí no podemos testear la cámara real (porque es del sistema),
        // pero verificamos que el clic se procesa sin crashear.
        composeTestRule.onNodeWithTag("boton_camara").assertExists()
    }
}
*/