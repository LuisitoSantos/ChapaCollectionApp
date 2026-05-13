// Archivo: MainActivity.kt
package com.tuempresa.chapacollectionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.tuempresa.chapacollectionapp.ui.screens.ChapaMapScreen
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel
import com.tuempresa.chapacollectionapp.ui.screens.LoginScreen
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModelFactory

/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val database = ChapaDatabase.getDatabase(this)
        //val repository = ChapaRepository(database.chapaDao())
        //val factory = ChapaViewModelFactory(repository)
        val supabaseService = com.tuempresa.chapacollectionapp.components.SupabaseService()
        val factory = ChapaViewModelFactory(supabaseService)

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
*/

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supabaseService = com.tuempresa.chapacollectionapp.components.SupabaseService()
        val chapaFactory = ChapaViewModelFactory(supabaseService)
        val authFactory = AuthViewModelFactory(supabaseService.client)

        setContent {
            ChapaCollectionAppTheme {
                // 1. ESTE ES EL CONTROLADOR PRINCIPAL (Login vs App)
                val rootNavController = rememberNavController()
                val chapaViewModel: ChapaViewModel = viewModel(factory = chapaFactory)
                val authViewModel: AuthViewModel = viewModel(factory = authFactory)

                val currentUser = authViewModel.currentUser
                val startDest = if (currentUser != null) "app_main" else "login"

                NavHost(
                    navController = rootNavController,
                    startDestination = startDest
                ) {
                    // PANTALLA DE LOGIN
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                rootNavController.navigate("app_main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // TODA LA APP (Con su propia navegación interna)
                    composable("app_main") {
                        MainAppContent(chapaViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(chapaViewModel: ChapaViewModel) {
    // 2. ESTE CONTROLADOR ES SOLO PARA LAS PESTAÑAS (Lista, Mapa, etc.)
    val snackNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by snackNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Anadir).forEach { screen ->
                    BottomNavigationItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            snackNavController.navigate(screen.route) {
                                popUpTo(snackNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(screen.label) },
                        icon = {
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
            navController = snackNavController,
            startDestination = Screen.Lista.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lista.route) { ChapaListScreen(chapaViewModel, snackNavController) }
            composable(Screen.Mapa.route) { ChapaMapScreen(chapaViewModel) }
            composable(Screen.Buscar.route) { SearchChapaScreen(chapaViewModel, snackNavController) }
            composable(Screen.Anadir.route) { AddChapaScreen(chapaViewModel, snackNavController) }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Solo mostramos la barra si la ruta actual es una de las pestañas
    val screens = listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Anadir)
    if (screens.any { it.route == currentRoute }) {
        BottomNavigation {
            screens.forEach { screen ->
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
}