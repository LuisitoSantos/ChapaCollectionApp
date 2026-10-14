// Archivo: MainActivity.kt
package com.tuempresa.chapacollectionapp

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuempresa.chapacollectionapp.navigation.Screen
import com.tuempresa.chapacollectionapp.ui.screens.AddChapaScreen
import com.tuempresa.chapacollectionapp.ui.screens.ChapaListScreen
import com.tuempresa.chapacollectionapp.ui.theme.ChapaCollectionAppTheme
import com.tuempresa.chapacollectionapp.ui.theme.ChapaBackground
import com.tuempresa.chapacollectionapp.ui.theme.ChapaOrange
import com.tuempresa.chapacollectionapp.ui.theme.ChapaSlate
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModelFactory
import androidx.compose.material.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Text
import com.tuempresa.chapacollectionapp.ui.screens.SearchChapaScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.Icon
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tuempresa.chapacollectionapp.ui.screens.ChapaMapScreen
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModel
import com.tuempresa.chapacollectionapp.ui.screens.LoginScreen
import com.tuempresa.chapacollectionapp.ui.components.BeerLoadingScreen
import com.tuempresa.chapacollectionapp.ui.components.MINIMUM_SPLASH_DURATION_MS
import com.tuempresa.chapacollectionapp.viewmodel.AuthViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.first

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

                            listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Comunidad).forEach { screen ->
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
                                            Screen.Comunidad -> Icons.Default.Groups
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

                // Esperamos a que Supabase termine de restaurar la sesión local.
                var checkingAuth by remember { mutableStateOf(true) }
                var userLoggedIn by remember { mutableStateOf(false) }
                val startupStartedAt = remember { SystemClock.elapsedRealtime() }

                LaunchedEffect(Unit) {
                    supabaseService.client.auth.sessionStatus.first {
                        it !is SessionStatus.LoadingFromStorage
                    }
                    userLoggedIn = authViewModel.isUserLoggedIn()
                    if (userLoggedIn) {
                        chapaViewModel.cargarChapasDeSupabase(
                            showInitialLoading = true,
                            loadingStartedAt = startupStartedAt
                        )
                        snapshotFlow { chapaViewModel.initialChapasLoaded }.first { it }
                    }
                    val remainingSplashTime = MINIMUM_SPLASH_DURATION_MS -
                        (SystemClock.elapsedRealtime() - startupStartedAt)
                    if (remainingSplashTime > 0) kotlinx.coroutines.delay(remainingSplashTime)
                    checkingAuth = false
                }

                if (checkingAuth) {
                    BeerLoadingScreen(isLoading = true)
                } else {
                    // Una vez comprobado, cargamos el NavHost normal
                    NavHost(
                        navController = rootNavController,
                        startDestination = if (userLoggedIn) "app_main" else "login"
                    ) {
                        // PANTALLA DE LOGIN
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    chapaViewModel.cargarChapasDeSupabase(showInitialLoading = true)
                                    rootNavController.navigate("app_main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // TODA LA APP (Con su propia navegación interna)
                        composable("app_main") {
                            MainAppContent(chapaViewModel, authViewModel, rootNavController)
                        }
                    }
                }
                /*
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
                        MainAppContent(chapaViewModel, authViewModel, rootNavController)
                    }
                }

                 */
            }
        }
    }
}

@Composable
fun MainAppContent(chapaViewModel: ChapaViewModel, authViewModel: AuthViewModel, rootNavController: NavController) {
    // 2. ESTE CONTROLADOR ES SOLO PARA LAS PESTAÑAS (Lista, Mapa, etc.)
    val snackNavController = rememberNavController()
    val currentRoute = snackNavController.currentBackStackEntryAsState().value?.destination?.route

    val loadingStartedAt = chapaViewModel.initialLoadingStartedAt
    var minimumSplashTimeElapsed by remember(loadingStartedAt) {
        mutableStateOf(
            loadingStartedAt == 0L ||
                SystemClock.elapsedRealtime() - loadingStartedAt >= MINIMUM_SPLASH_DURATION_MS
        )
    }
    LaunchedEffect(loadingStartedAt) {
        val remaining = MINIMUM_SPLASH_DURATION_MS -
            (SystemClock.elapsedRealtime() - loadingStartedAt)
        if (loadingStartedAt != 0L && remaining > 0) kotlinx.coroutines.delay(remaining)
        minimumSplashTimeElapsed = true
    }

    if (!chapaViewModel.initialChapasLoaded || !minimumSplashTimeElapsed) {
        BeerLoadingScreen(isLoading = true)
    } else Scaffold(
        backgroundColor = ChapaBackground,
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (currentRoute == Screen.Lista.route) {
                FloatingActionButton(
                    onClick = {
                        snackNavController.navigate(Screen.Anadir.route) {
                            launchSingleTop = true
                        }
                    },
                    backgroundColor = ChapaOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir chapa")
                }
            }
        },
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color.White,
                contentColor = ChapaOrange,
                elevation = 8.dp
            ) {
                listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Comunidad).forEach { screen ->
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
                        selectedContentColor = ChapaOrange,
                        unselectedContentColor = ChapaSlate,
                        icon = {
                            val icon = when(screen) {
                                Screen.Lista -> Icons.Default.List
                                Screen.Mapa -> Icons.Default.Public
                                Screen.Buscar -> Icons.Default.Search
                                Screen.Comunidad -> Icons.Default.Groups
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
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Se conserva vivo el MapView al cambiar de pestaña. Así no descarga y crea
            // el mapa de nuevo en cada visita, y puede ocultarse antes de mostrar otra vista.
            ChapaMapScreen(chapaViewModel, isVisible = currentRoute == Screen.Mapa.route)

            if (currentRoute != Screen.Mapa.route) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                )
            }

            NavHost(
                navController = snackNavController,
                startDestination = Screen.Lista.route,
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Lista.route) {
                    ChapaListScreen(
                        chapaViewModel,
                        authViewModel,
                        snackNavController,
                        onLogout = {
                            rootNavController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // El mapa persistente de la capa inferior dibuja esta ruta.
                composable(Screen.Mapa.route) { }
                composable(Screen.Buscar.route) { SearchChapaScreen(chapaViewModel, snackNavController) }
                composable(Screen.Comunidad.route) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Comunidad · Próximamente")
                    }
                }
                composable(Screen.Anadir.route) { AddChapaScreen(chapaViewModel, snackNavController) }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Solo mostramos la barra si la ruta actual es una de las pestañas
    val screens = listOf(Screen.Lista, Screen.Mapa, Screen.Buscar, Screen.Comunidad)
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
                            Screen.Comunidad -> Icons.Default.Groups
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
