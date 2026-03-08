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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/*
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

                    //para poner la info arriba a la derecha
                    map.uiSettings.apply {
                        // Mover el logo de MapLibre (el texto de la izquierda)
                        setLogoGravity(android.view.Gravity.TOP or android.view.Gravity.END)
                        setLogoMargins(0, 40, 40, 0) // Márgenes: Izq, Top, Der, Bottom

                        // Mover el botón de información "i" (Atribución)
                        setAttributionGravity(android.view.Gravity.TOP or android.view.Gravity.END)
                        setAttributionMargins(0, 40, 290, 0)
                    }

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

 */

@Composable
fun ChapaMapScreen(viewModel: ChapaViewModel) {
    val chapas by viewModel.allChapas.observeAsState(emptyList())
    val styleUrl = "https://api.maptiler.com/maps/basic-v2/style.json?key=jvSAqJlbbKZIBqsyrioy"
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    // Estado para la chapa que el usuario pulsa en el mapa
    var chapaSeleccionada by remember { mutableStateOf<Chapa?>(null) }
    var chapasEnCluster by remember { mutableStateOf<List<Chapa>>(emptyList()) }


    // Estado para controlar si la leyenda está expandida
    var leyendaExpandida by remember { mutableStateOf(false) }

    // Cálculo del ranking de países
    val rankingPaises = remember(chapas) {
        chapas.groupBy { it.pais }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. EL MAPA (Tu código actual)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapLibre.getInstance(context)
                MapView(context).apply {
                    getMapAsync { map ->
                        mapInstance = map

                        map.addOnMapClickListener { latLng ->
                            val point = map.projection.toScreenLocation(latLng)

                            val individualFeatures = map.queryRenderedFeatures(point, "chapa-imagen-layer")
                            val clusterFeatures = map.queryRenderedFeatures(point, "circulos-marcadores")

                            when {
                                individualFeatures.isNotEmpty() -> {
                                    val nombre = individualFeatures[0].getStringProperty("nombre")
                                    chapaSeleccionada = chapas.find { it.nombre == nombre }
                                    chapasEnCluster = emptyList()
                                }
                                clusterFeatures.isNotEmpty() -> {
                                    val feature = clusterFeatures[0]
                                    val geometry = feature.geometry()
                                    if (geometry is org.maplibre.geojson.Point) {
                                        val lon = geometry.longitude()
                                        val lat = geometry.latitude()

                                        chapasEnCluster = chapas.filter {
                                            // Usamos !! porque si la chapa está en el mapa, tiene coordenadas.
                                            // O it.latitud ?: 0.0 si prefieres más seguridad.
                                            Math.abs((it.latitud ?: 0.0) - lat) < 0.001 &&
                                                    Math.abs((it.longitud ?: 0.0) - lon) < 0.001
                                        }
                                    }
                                    chapaSeleccionada = null
                                }
                                else -> {
                                    chapaSeleccionada = null
                                    chapasEnCluster = emptyList()
                                }
                            }
                            true
                        }

                        map.uiSettings.apply {
                            setLogoGravity(android.view.Gravity.TOP or android.view.Gravity.START)
                            setLogoMargins(40, 40, 0, 0)
                            setAttributionGravity(android.view.Gravity.TOP or android.view.Gravity.START)
                            setAttributionMargins(295, 40, 0, 0)
                        }
                        map.setStyle(styleUrl) { style ->
                            // ... tu lógica de idioma ...
                            actualizarMarcadores(map, style, chapas)
                        }
                    }
                }
            }
        )

        // 2. LA LEYENDA (Superpuesta abajo a la izquierda)
        androidx.compose.material3.Card(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomStart)
                .padding(16.dp)
                .widthIn(max = 175.dp) // Limitamos el ancho máximo de la tarjeta
                .animateContentSize(), // Animación suave al abrir/cerrar
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(8.dp),
            onClick = { leyendaExpandida = !leyendaExpandida }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Título / Total (Siempre visible)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFF2196F3)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = "Total: ${chapas.size}",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                // Ranking (Solo visible si está expandido)
                if (leyendaExpandida) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider()
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

                    /*
                    androidx.compose.material3.Text(
                        text = "Ranking por países:",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                     */

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))

                    // LISTA CON SCROLL
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .heightIn(max = 150.dp) // Altura máxima antes de permitir scroll
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        rankingPaises.forEach { (pais, cantidad) ->
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Text(
                                    text = pais,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f) // El nombre ocupa el espacio disponible
                                )
                                androidx.compose.material3.Text(
                                    text = "x$cantidad",
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. NUEVA: TARJETA DE DETALLE DE CHAPA (Aparece al pulsar una imagen)
        chapaSeleccionada?.let { chapa ->
            androidx.compose.material3.Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd) // Aparece en el lado derecho
                    .padding(16.dp)
                    .width(200.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detalle",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        // Botón para cerrar la info
                        IconButton(
                            onClick = { chapaSeleccionada = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                modifier = Modifier.scale(0.5f), // Aquí es donde se aplica la escala
                                tint = Color.Gray
                            )
                        }
                    }

                    // Miniatura de la chapa
                    if (!chapa.imagePath.isNullOrEmpty()) {
                        AsyncImage(
                            model = chapa.imagePath,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(text = chapa.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${chapa.ciudad ?: ""}, ${chapa.pais}", style = MaterialTheme.typography.bodySmall)

                    if (chapa.anio != null) {
                        Text(text = "Año: ${chapa.anio}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 4. TARJETA PARA GRUPOS DE CHAPAS
        if (chapasEnCluster.isNotEmpty()) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .width(220.dp)
                    .heightIn(max = 300.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            //text = "${chapasEnCluster[0].ciudad ?: chapasEnCluster[0].pais}",
                            text = "Varios",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        IconButton(onClick = { chapasEnCluster = emptyList() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.scale(0.7f))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        chapasEnCluster.forEach { chapa ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Miniatura muy pequeña
                                if (!chapa.imagePath.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = chapa.imagePath,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(text = chapa.nombre, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
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
 */

/*
private fun actualizarMarcadores(map: MapLibreMap, style: Style, chapas: List<Chapa>) {
    val sourceId = "chapas-source"
    val colorAzul = android.graphics.Color.parseColor("#2196F3")
    val radioMarcador = 12f

    // 1. Crear el GeoJSON incluyendo el path de la imagen
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
                    // Importante: Guardamos el ID de la imagen para usarlo luego
                    addProperty("imagen_id", "img_${chapa.id}")
                })
            }
            features.add(feature)

            // 2. CARGAR LA IMAGEN EN EL ESTILO
            // Solo si la chapa tiene imagen y no está cargada ya
            if (!chapa.imagePath.isNullOrEmpty() && style.getImage("img_${chapa.id}") == null) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(chapa.imagePath)
                if (bitmap != null) {
                    // Redimensionamos la imagen para que no sea gigante en el mapa
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 150, 150, false)
                    style.addImage("img_${chapa.id}", scaledBitmap)
                }
            }
        }
        add("features", features)
    }

    // 3. Actualizar o crear fuente
    val existingSource = style.getSource(sourceId) as? GeoJsonSource
    if (existingSource != null) {
        existingSource.setGeoJson(featureCollection.toString())
    } else {
        val geoJsonSource = GeoJsonSource(sourceId, featureCollection.toString(),
            org.maplibre.android.style.sources.GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(14)
                .withClusterRadius(50)
        )
        style.addSource(geoJsonSource)
    }

    // 4. CAPA DE CÍRCULOS (Solo para Clusters > 1)
    // Quitamos el círculo azul si es una chapa individual para que no tape la imagen
    if (style.getLayer("circulos-marcadores") == null) {
        style.addLayer(org.maplibre.android.style.layers.CircleLayer("circulos-marcadores", sourceId).apply {
            setProperties(
                circleColor(colorAzul),
                circleRadius(radioMarcador),
                circleStrokeColor(android.graphics.Color.WHITE),
                circleStrokeWidth(1.5f)
            )
            // Solo se muestra si es un cluster
            setFilter(org.maplibre.android.style.expressions.Expression.has("point_count"))
        })
    }

    // 5. NÚMERO PARA CLUSTERS
    if (style.getLayer("cluster-count") == null) {
        style.addLayer(SymbolLayer("cluster-count", sourceId).apply {
            setProperties(
                textField(org.maplibre.android.style.expressions.Expression.get("point_count")),
                textSize(10f),
                textColor(android.graphics.Color.WHITE),
                textIgnorePlacement(true),
                textAllowOverlap(true)
            )
            setFilter(org.maplibre.android.style.expressions.Expression.has("point_count"))
        })
    }

    // 6. NUEVA CAPA: IMAGEN PARA CHAPAS INDIVIDUALES (Cuando hay 1)
    if (style.getLayer("chapa-imagen-layer") == null) {
        style.addLayer(SymbolLayer("chapa-imagen-layer", sourceId).apply {
            setProperties(
                // Dinámicamente busca la imagen por el ID que guardamos en las propiedades
                iconImage(org.maplibre.android.style.expressions.Expression.get("imagen_id")),
                iconSize(0.6f), // Ajusta este valor según quieras la chapa de grande
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
            // Solo se muestra si NO es un cluster
            setFilter(org.maplibre.android.style.expressions.Expression.not(
                org.maplibre.android.style.expressions.Expression.has("point_count")
            ))
        })
    }
}
 */

private fun actualizarMarcadores(map: MapLibreMap, style: Style, chapas: List<Chapa>) {
    val sourceId = "chapas-source"
    val colorAzul = android.graphics.Color.parseColor("#2196F3")

    // 1. Agrupamos chapas por coordenadas exactas
    val chapasPorCoordenada = chapas.groupBy { "${it.latitud},${it.longitud}" }

    val featureCollection = JsonObject().apply {
        addProperty("type", "FeatureCollection")
        val features = JsonArray()

        chapasPorCoordenada.forEach { (_, listaEnMismoPunto) ->
            listaEnMismoPunto.forEachIndexed { index, chapa ->
                val feature = JsonObject().apply {
                    addProperty("type", "Feature")
                    add("geometry", JsonObject().apply {
                        addProperty("type", "Point")
                        add("coordinates", JsonArray().apply {
                            // Dispersión circular si coinciden en el mismo punto
                            val desplazamiento = if (listaEnMismoPunto.size > 1) {
                                val angulo = 2.0 * Math.PI * index / listaEnMismoPunto.size
                                val radio = 0.300 // Radio de separación sutil
                                Pair(Math.cos(angulo) * radio, Math.sin(angulo) * radio)
                            } else Pair(0.0, 0.0)

                            add((chapa.longitud ?: 0.0) + desplazamiento.first)
                            add((chapa.latitud ?: 0.0) + desplazamiento.second)
                        })
                    })
                    add("properties", JsonObject().apply {
                        addProperty("nombre", chapa.nombre)
                        addProperty("imagen_id", "img_${chapa.id}")
                    })
                }
                features.add(feature)

                if (!chapa.imagePath.isNullOrEmpty() && style.getImage("img_${chapa.id}") == null) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(chapa.imagePath)
                    bitmap?.let {
                        val scaled = android.graphics.Bitmap.createScaledBitmap(it, 150, 150, false)
                        style.addImage("img_${chapa.id}", scaled)
                    }
                }
            }
        }
        add("features", features)
    }

    val existingSource = style.getSource(sourceId) as? GeoJsonSource
    if (existingSource != null) {
        existingSource.setGeoJson(featureCollection.toString())
    } else {
        style.addSource(GeoJsonSource(sourceId, featureCollection.toString(),
            org.maplibre.android.style.sources.GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(3)
                .withClusterRadius(50)
        ))
    }

    // --- CAPA 1: Círculos Azules ---
    // Se muestra si es un cluster O si es individual con zoom < 4
    if (style.getLayer("circulos-marcadores") == null) {
        style.addLayer(org.maplibre.android.style.layers.CircleLayer("circulos-marcadores", sourceId).apply {
            setProperties(
                org.maplibre.android.style.layers.PropertyFactory.circleColor(colorAzul),
                org.maplibre.android.style.layers.PropertyFactory.circleRadius(12f),
                org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f)
            )
            setFilter(
                org.maplibre.android.style.expressions.Expression.any(
                    org.maplibre.android.style.expressions.Expression.has("point_count"),
                    org.maplibre.android.style.expressions.Expression.lt(org.maplibre.android.style.expressions.Expression.zoom(), 4f)
                )
            )
        })
    }

    // --- CAPA 2: Texto (Números) ---
    // Muestra el número del cluster o un "1" si es individual con zoom < 4
    if (style.getLayer("cluster-count") == null) {
        style.addLayer(SymbolLayer("cluster-count", sourceId).apply {
            setProperties(
                org.maplibre.android.style.layers.PropertyFactory.textField(
                    org.maplibre.android.style.expressions.Expression.coalesce(
                        org.maplibre.android.style.expressions.Expression.get("point_count"),
                        org.maplibre.android.style.expressions.Expression.literal("1")
                    )
                ),
                org.maplibre.android.style.layers.PropertyFactory.textSize(12f),
                org.maplibre.android.style.layers.PropertyFactory.textColor(android.graphics.Color.WHITE)
            )
            setFilter(
                org.maplibre.android.style.expressions.Expression.any(
                    org.maplibre.android.style.expressions.Expression.has("point_count"),
                    org.maplibre.android.style.expressions.Expression.lt(org.maplibre.android.style.expressions.Expression.zoom(), 4f)
                )
            )
        })
    }

    // --- CAPA 3: Imágenes Reales ---
    // Se activan solo si NO es cluster Y el zoom es >= 4
    if (style.getLayer("chapa-imagen-layer") == null) {
        style.addLayer(SymbolLayer("chapa-imagen-layer", sourceId).apply {
            setProperties(
                org.maplibre.android.style.layers.PropertyFactory.iconImage(org.maplibre.android.style.expressions.Expression.get("imagen_id")),
                org.maplibre.android.style.layers.PropertyFactory.iconSize(0.5f),
                org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true)
            )
            setFilter(
                org.maplibre.android.style.expressions.Expression.all(
                    org.maplibre.android.style.expressions.Expression.not(org.maplibre.android.style.expressions.Expression.has("point_count")),
                    org.maplibre.android.style.expressions.Expression.gte(org.maplibre.android.style.expressions.Expression.zoom(), 4f)
                )
            )
        })
    }
}