// Archivo: MainActivity.kt
package com.tuempresa.chapacollectionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.repository.ChapaRepository
import com.tuempresa.chapacollectionapp.ui.screens.AddChapaScreen
import com.tuempresa.chapacollectionapp.ui.screens.ChapaListScreen
import com.tuempresa.chapacollectionapp.ui.theme.ChapaCollectionAppTheme
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModelFactory
import com.tuempresa.chapacollectionapp.data.ChapaDatabase
import androidx.compose.material.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos la base de datos y el repositorio
        val database = ChapaDatabase.getDatabase(this)
        val repository = ChapaRepository(database.chapaDao())
        val factory = ChapaViewModelFactory(repository)

        setContent {
            ChapaCollectionAppTheme {
                val navController = rememberNavController()
                val viewModel: ChapaViewModel = viewModel(factory = factory)

                // Scaffold con barra de navegación inferior
                Scaffold(
                    bottomBar = {
                        BottomNavigation {
                            val currentRoute =
                                navController.currentBackStackEntryAsState().value?.destination?.route
                            listOf(Screen.Lista, Screen.Anadir).forEach { screen ->
                                BottomNavigationItem(
                                    selected = currentRoute == screen.route,
                                    onClick = { navController.navigate(screen.route) },
                                    label = { Text(screen.label) },
                                    icon = {}
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Lista.route,
                        modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Lista.route) {
                            ChapaListScreen(viewModel, navController)
                        }
                        composable(Screen.Anadir.route) {
                            AddChapaScreen(viewModel, navController)
                        }
                    }
                }
            }
        }
    }
}
