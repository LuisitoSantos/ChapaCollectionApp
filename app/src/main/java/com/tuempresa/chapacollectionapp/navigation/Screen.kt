package com.tuempresa.chapacollectionapp.navigation

sealed class Screen(val route: String, val label: String) {
    object Lista : Screen("lista", "Chapas")
    object Buscar : Screen("buscar", "Buscar")
    object Anadir : Screen("anadir", "Añadir")
    // Puedes añadir más pantallas aquí si las necesitas
}
