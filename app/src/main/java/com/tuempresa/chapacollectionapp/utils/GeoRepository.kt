package com.tuempresa.chapacollectionapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.compose.ui.graphics.vector.path
import java.io.File
import java.io.FileOutputStream

class GeoRepository(private val context: Context) {
    // Obtenemos la ruta correcta donde Android guarda las bases de datos
    private val dbName = "geonames.db"
    private val dbFile = context.getDatabasePath(dbName)

    init {
        copyDatabaseIfNeeded()
    }

    private fun copyDatabaseIfNeeded() {
        if (!dbFile.exists()) {
            try {
                // 1. Listar los archivos que Android VE en assets para depurar
                val assetsList = context.assets.list("") ?: emptyArray()
                Log.d("GEO_SQL", "Archivos encontrados en assets: ${assetsList.joinToString()}")

                if (!assetsList.contains(dbName)) {
                    Log.e("GEO_SQL", "❌ ERROR: El archivo $dbName NO está en la carpeta assets.")
                    return
                }

                // 2. Intentar la copia
                dbFile.parentFile?.mkdirs()
                context.assets.open(dbName).use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("GEO_SQL", "✅ Base de datos copiada con éxito")
            } catch (e: Exception) {
                Log.e("GEO_SQL", "❌ Error crítico al copiar: ${e.stackTraceToString()}")
            }
        }
    }

    fun getCoordinates(pais: String, ciudad: String?): Pair<Double, Double>? {
        if (!dbFile.exists()) {
            Log.e("GEO_SQL", "❌ El archivo de base de datos no existe en: ${dbFile.path}")
            return null
        }

        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        var coords: Pair<Double, Double>? = null

        val paisBusqueda = pais.trim()
        val ciudadBusqueda = ciudad?.trim()

        val query: String
        val args: Array<String>

        if (ciudadBusqueda.isNullOrBlank()) {
            // CORREGIDO: Añadida 'longitude' y quitada coma sobrante
            query = "SELECT latitude, longitude FROM locations WHERE country_name LIKE ? AND feature_code = 'PPLC' LIMIT 1"
            args = arrayOf("%$paisBusqueda%")
            Log.d("GEO_SQL", "Buscando CAPITAL de: $paisBusqueda")
        } else {
            query = "SELECT latitude, longitude FROM locations WHERE name LIKE ? AND country_name LIKE ? LIMIT 1"
            args = arrayOf(ciudadBusqueda, "%$paisBusqueda%")
            Log.d("GEO_SQL", "Buscando CIUDAD: $ciudadBusqueda en PAÍS: $paisBusqueda")
        }

        try {
            db.rawQuery(query, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    val lat = cursor.getDouble(0)
                    val lon = cursor.getDouble(1)
                    coords = Pair(lat, lon)
                    Log.d("GEO_SQL", "✅ ¡ENCONTRADO!: Lat=$lat, Lon=$lon")
                } else {
                    Log.e("GEO_SQL", "❌ NO ENCONTRADO en SQLite. Revisa si el nombre está en inglés en la DB.")
                }
            }
        } catch (e: Exception) {
            Log.e("GEO_SQL", "Error ejecutando la consulta: ${e.message}")
        } finally {
            db.close()
        }
        return coords
    }
}