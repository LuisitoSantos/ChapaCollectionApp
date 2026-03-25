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
import com.tuempresa.chapacollectionapp.ui.screens.AddChapaScreen
import com.tuempresa.chapacollectionapp.ui.screens.ChapaListScreen
import com.tuempresa.chapacollectionapp.ui.theme.ChapaCollectionAppTheme
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModelFactory
import androidx.compose.material.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Text
import com.tuempresa.chapacollectionapp.ui.screens.SearchChapaScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.Icon
import androidx.compose.material.icons.filled.Public
import com.tuempresa.chapacollectionapp.ui.screens.ChapaMapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val database = ChapaDatabase.getDatabase(this)
        //val repository = ChapaRepository(database.chapaDao())
        //val factory = ChapaViewModelFactory(repository)
        val firebaseService = com.tuempresa.chapacollectionapp.components.FirebaseService()
        val factory = ChapaViewModelFactory(firebaseService)

        setContent {
            ChapaCollectionAppTheme {
                val navController = rememberNavController()
                val viewModel: ChapaViewModel = viewModel(factory = factory)

                Scaffold(
                    bottomBar = {
                        BottomNavigation {
                            val navBackStackEntry = navController.currentBackStackEntryAsState().value
                            val currentRoute = navBackStackEntry?.destination?.route

                            listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Anadir).forEach { screen ->
                                BottomNavigationItem(
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    label = { Text(screen.label) },
                                    icon = {
                                        // Añadimos iconos para que se vea mejor
                                        val icon = when(screen) {
                                            Screen.Lista -> Icons.Default.List
                                            Screen.Mapa -> Icons.Default.Public
                                            Screen.Buscar -> Icons.Default.Search
                                            Screen.Anadir -> Icons.Default.Add
                                            else -> Icons.Default.Search
                                        }
                                        Icon(icon, contentDescription = null)
                                    }
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

                        composable(Screen.Mapa.route) {
                            ChapaMapScreen(viewModel)
                        }

                        // CAMBIO AQUÍ: Llamamos a la nueva pantalla
                        composable(Screen.Buscar.route) {
                            SearchChapaScreen(viewModel, navController)
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
