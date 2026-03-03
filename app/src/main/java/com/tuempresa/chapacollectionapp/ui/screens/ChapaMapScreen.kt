package com.tuempresa.chapacollectionapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.shapes.Feature
import com.tuempresa.chapacollectionapp.viewmodel.ChapaViewModel
import com.tuempresa.chapacollectionapp.utils.GeoData
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.tuempresa.chapacollectionapp.data.Chapa
import org.maplibre.geojson.Point

@Composable
fun ChapaMapScreen(viewModel: ChapaViewModel) {
    // Observamos las chapas: esto disparará recomposiciones al añadir/borrar/editar
    val chapas by viewModel.allChapas.observeAsState(emptyList())
    //val styleUrl = "https://demotiles.maplibre.org/style.json"
    val styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=jvSAqJlbbKZIBqsyrioy"
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // EFECTO DE SINCRONIZACIÓN: Este es el motor de actualización
    LaunchedEffect(chapas, mapInstance) {
        val map = mapInstance
        if (map != null) {
            // Verificamos si el estilo ya cargó antes de actualizar
            map.getStyle { style ->
                Log.d("MAP_DEBUG", "Actualizando marcadores. Total: ${chapas.size}")
                actualizarMarcadores(map, style, chapas)
            }
        }
    }

    /*
    //Para usar maplibre
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapLibre.getInstance(context)
            MapView(context).apply {
                getMapAsync { map ->
                    mapInstance = map
                    map.setStyle(styleUrl) { style ->
                        // Centro inicial (Mundo)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 1.0))
                        actualizarMarcadores(map, style, chapas)
                    }
                }
            }
        },
        update = { /* Dejamos que LaunchedEffect maneje la lógica para evitar colisiones */ }
    )
     */


    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapLibre.getInstance(context)
            MapView(context).apply {
                getMapAsync { map ->
                    mapInstance = map
                    map.setStyle(styleUrl) { style ->

                        // --- AQUÍ CAMBIAMOS EL IDIOMA A ESPAÑOL ---
                        for (layer in style.layers) {
                            if (layer is SymbolLayer) {
                                layer.setProperties(
                                    textField(
                                        org.maplibre.android.style.expressions.Expression.coalesce(
                                            org.maplibre.android.style.expressions.Expression.get("name:es"), // Intenta español
                                            org.maplibre.android.style.expressions.Expression.get("name:latin"), // Si no, alfabeto latino
                                            org.maplibre.android.style.expressions.Expression.get("name") // Por defecto
                                        )
                                    )
                                )
                            }
                        }

                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 1.0))
                        actualizarMarcadores(map, style, chapas)
                    }
                }
            }
        }
    )


}

private fun actualizarMarcadores(map: MapLibreMap, style: Style, chapas: List<Chapa>) {
    val sourceId = "chapas-source"
    val colorAzul = android.graphics.Color.parseColor("#2196F3")
    val radioMarcador = 12f // Tamaño más pequeño y manejable

    // 1. Crear el GeoJSON
    val featureCollection = JsonObject().apply {
        addProperty("type", "FeatureCollection")
        val features = JsonArray()
        chapas.forEach { chapa ->
            val feature = JsonObject().apply {
                addProperty("type", "Feature")
                add("geometry", JsonObject().apply {
                    addProperty("type", "Point")
                    add("coordinates", JsonArray().apply {
                        add(chapa.longitud)
                        add(chapa.latitud)
                    })
                })
                add("properties", JsonObject().apply {
                    addProperty("nombre", chapa.nombre)
                })
            }
            features.add(feature)
        }
        add("features", features)
    }

    // 2. Actualizar fuente si ya existe
    val existingSource = style.getSource(sourceId) as? GeoJsonSource
    if (existingSource != null) {
        existingSource.setGeoJson(featureCollection.toString())
        return
    }

    // 3. Configurar Fuente con Clustering
    val geoJsonSource = GeoJsonSource(sourceId, featureCollection.toString(),
        org.maplibre.android.style.sources.GeoJsonOptions()
            .withCluster(true)
            .withClusterMaxZoom(14)
            .withClusterRadius(50)
    )
    style.addSource(geoJsonSource)

    // --- CAPAS DE CÍRCULOS UNIFICADAS ---

    // 4. Círculos (Iguales para clusters y puntos individuales)
    // Creamos una sola capa para el fondo del marcador para que todos se vean igual
    style.addLayer(org.maplibre.android.style.layers.CircleLayer("circulos-marcadores", sourceId).apply {
        setProperties(
            circleColor(colorAzul),
            circleRadius(radioMarcador),
            circleStrokeColor(android.graphics.Color.WHITE),
            circleStrokeWidth(1.5f) // Borde un poco más fino al ser más pequeño
        )
    })

    // --- CAPAS DE TEXTO (NÚMEROS) ---

    // 5. Número para Clusters
    style.addLayer(SymbolLayer("cluster-count", sourceId).apply {
        setProperties(
            textField(org.maplibre.android.style.expressions.Expression.get("point_count")),
            textSize(10f), // Texto un poco más pequeño para que quepa
            textColor(android.graphics.Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        setFilter(org.maplibre.android.style.expressions.Expression.has("point_count"))
    })

    // 6. Número "1" para Puntos Individuales
    style.addLayer(SymbolLayer("puntos-individuales-numero", sourceId).apply {
        setProperties(
            textField("1"),
            textSize(10f),
            textColor(android.graphics.Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        setFilter(org.maplibre.android.style.expressions.Expression.not(org.maplibre.android.style.expressions.Expression.has("point_count")))
    })
}