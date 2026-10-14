package com.tuempresa.chapacollectionapp.navigation

sealed class Screen(val route: String, val label: String) {
    object Lista : Screen("lista", "Colección")
    object Mapa : Screen("mapa", "Mapa")
    object Buscar : Screen("buscar", "Buscador")
    object Comunidad : Screen("comunidad", "Comunidad")
    object Anadir : Screen("anadir", "Añadir")
    // Puedes añadir más pantallas aquí si las necesitas
}
