package com.tuempresa.chapacollectionapp.utils

import org.maplibre.android.geometry.LatLng

object GeoData {
    // Mapa de País -> Coordenadas de la Capital
    val paises = mapOf(
        "España" to LatLng(40.4167, -3.7033), // Madrid
        "Francia" to LatLng(48.8566, 2.3522), // París
        "México" to LatLng(19.4326, -99.1332), // CDMX
        "Argentina" to LatLng(-34.6037, -58.3816), // Buenos Aires
        // Añade aquí los países que necesites
    )

    // Mapa opcional de Ciudad -> Coordenadas
    val ciudades = mapOf(
        "Barcelona" to LatLng(41.3851, 2.1734),
        "Monterrey" to LatLng(25.6866, -100.3161),
    )

    fun getCoordinates(pais: String, ciudad: String?): LatLng {
        return ciudades[ciudad] ?: paises[pais] ?: LatLng(0.0, 0.0) // 0,0 si no existe
    }

    fun getCoordinates1(pais: String): LatLng {
        return paises[pais] ?: LatLng(0.0, 0.0) // 0,0 si no existe
    }
}