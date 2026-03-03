/*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditChapaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Simulamos una chapa para editar
    private val chapaDePrueba = Chapa(
        id = 1,
        nombre = "Chapa Antigua",
        anio = "1995",
        imagePath = null
    )

    private fun startScreen() {
        composeTestRule.setContent {
            EditChapaScreen(
                navController = rememberNavController(),
                chapaId = chapaDePrueba.id // Pasamos el ID de la chapa a editar
                // Si usas un ViewModel, aquí pasarías el mock con la chapaDePrueba ya cargada
            )
        }
    }

    @Test
    fun verificarDatosCargadosCorrectamente() {
        startScreen()

        // El test verifica que al abrir la pantalla, los campos NO están vacíos
        // sino que tienen los datos de la chapa que queremos editar
        composeTestRule.onNodeWithTag("campo_nombre_edit").assertTextEquals("Chapa Antigua")
        composeTestRule.onNodeWithTag("campo_anio_edit").assertTextEquals("1995")
    }

    @Test
    fun modificarCamposYNavegar() {
        startScreen()

        // Borramos el nombre anterior y escribimos uno nuevo
        composeTestRule.onNodeWithTag("campo_nombre_edit").performTextReplacement("Chapa Actualizada")

        // Modificamos el año
        composeTestRule.onNodeWithTag("campo_anio_edit").performTextReplacement("2024")

        // Verificamos los cambios en la UI
        composeTestRule.onNodeWithTag("campo_nombre_edit").assertTextEquals("Chapa Actualizada")
        composeTestRule.onNodeWithTag("campo_anio_edit").assertTextEquals("2024")

        // Pulsamos guardar
        composeTestRule.onNodeWithTag("boton_guardar_edit").performClick()
    }

    @Test
    fun verificarBotonGuardarEstaPresente() {
        startScreen()

        // Verificamos que el botón de guardar sea visible y clicable
        composeTestRule.onNodeWithTag("boton_guardar_edit")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}

 */