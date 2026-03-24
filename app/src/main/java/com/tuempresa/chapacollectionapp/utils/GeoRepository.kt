package com.tuempresa.chapacollectionapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.FileOutputStream
import kotlin.text.equals
import java.util.Locale

class GeoRepository(private val context: Context) {
    // Obtenemos la ruta correcta donde Android guarda las bases de datos
    private val dbName = "geonames.db"
    private val dbFile = context.getDatabasePath(dbName)

    init {
        copyDatabaseIfNeeded()
    }

    private fun copyDatabaseIfNeeded() {
        // COMENTA O QUITA EL IF para forzar la copia del nuevo geonames.db
        //if (!dbFile.exists()) {
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
        //}
    }

    fun getCoordinates(paisEspañol: String, ciudad: String?): Pair<Double, Double>? {
        if (!dbFile.exists()) {
            Log.e("GEO_DEBUG", "❌ Error: El archivo de base de datos no existe en la ruta interna.")
            return null
        }

        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        var coords: Pair<Double, Double>? = null

        // --- LOG 1: Entrada original ---
        Log.d("GEO_DEBUG", "1. Entrada Usuario -> Pais: '$paisEspañol', Ciudad: '${ciudad ?: "N/A"}'")

        val paisIngles = traducirPaisAIngles(paisEspañol.trim())

        // --- LOG 2: Resultado de traducción ---
        Log.d("GEO_DEBUG", "2. Traducción Pais -> '$paisEspañol' se convirtió a '$paisIngles'")

        val ciudadBusqueda = ciudad?.trim() ?: ""

        val query: String
        val args: Array<String>

        if (ciudadBusqueda.isBlank()) {
            query = """
            SELECT lat, lng, city, country FROM ciudades 
            WHERE country = ? 
            ORDER BY (capital = 'primary') DESC, population DESC 
            LIMIT 1
        """.trimIndent()
            args = arrayOf(paisIngles)
        } else {
            query = """
            SELECT lat, lng, city, country FROM ciudades 
            WHERE (city = ? OR city_ascii = ?) AND country = ? 
            ORDER BY population DESC 
            LIMIT 1
        """.trimIndent()
            args = arrayOf(ciudadBusqueda, ciudadBusqueda, paisIngles)
        }

        // --- LOG 3: Query que se va a lanzar ---
        Log.d("GEO_DEBUG", "3. Lanzando Query: ${query.replace("\n", " ")} con argumentos: ${args.joinToString(", ")}")

        // --- LOG DE DEPURACIÓN DE TABLAS ---
        try {
            val c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val tablas = mutableListOf<String>()
            while (c.moveToNext()) { tablas.add(c.getString(0)) }
            c.close()
            Log.d("GEO_DEBUG", "Tablas reales en el archivo: ${tablas.joinToString()}")
        } catch (e: Exception) {
            Log.e("GEO_DEBUG", "No se pudo listar las tablas")
        }

        try {
            db.rawQuery(query, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    val lat = cursor.getDouble(0)
                    val lng = cursor.getDouble(1)
                    val cityFound = cursor.getString(2)
                    val countryFound = cursor.getString(3)
                    coords = Pair(lat, lng)

                    // --- LOG 4: Éxito ---
                    Log.d("GEO_DEBUG", "✅ 4. ¡ÉXITO! Encontrado en DB: $cityFound, $countryFound ($lat, $lng)")
                } else {
                    // --- LOG 5: No hay filas ---
                    Log.w("GEO_DEBUG", "⚠️ 4. SQL: La consulta no devolvió ningún resultado. Comprueba si '$paisIngles' existe exactamente así en tu CSV.")
                }
            }
        } catch (e: Exception) {
            Log.e("GEO_DEBUG", "❌ 4. Error SQL: ${e.message}")
        } finally {
            db.close()
        }
        return coords
    }

    private fun traducirPaisAIngles(nombrePais: String): String {
        val locales = Locale.getAvailableLocales()
        for (locale in locales) {
            val nameInLocale = locale.getDisplayCountry(Locale("es")) // Intenta buscar el nombre en español
            if (nameInLocale.equals(nombrePais, ignoreCase = true)) {
                val translated = locale.getDisplayCountry(Locale.ENGLISH)
                return translated
            }

            // Segunda oportunidad: comparar con el nombre por defecto del sistema
            if (locale.displayCountry.equals(nombrePais, ignoreCase = true)) {
                return locale.getDisplayCountry(Locale.ENGLISH)
            }
        }
        Log.w("GEO_DEBUG", "Traducción: No se encontró traducción para '$nombrePais', usando original.")
        return nombrePais
    }

    // Añade esto a tu clase GeoRepository
    fun getCitySuggestions(query: String, paisEspañol: String): List<String> {
        if (query.length < 2) return emptyList() // No buscar hasta que haya 2 letras

        val paisIngles = traducirPaisAIngles(paisEspañol.trim())
        val suggestions = mutableListOf<String>()

        val db = try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            return emptyList()
        }

        try {
            // Buscamos ciudades que empiecen por la query en ese país
            val sql = "SELECT city FROM ciudades WHERE country = ? AND (city LIKE ? OR city_ascii LIKE ?) ORDER BY population DESC LIMIT 5"
            db.rawQuery(sql, arrayOf(paisIngles, "$query%", "$query%")).use { cursor ->
                while (cursor.moveToNext()) {
                    suggestions.add(cursor.getString(0))
                }
            }
        } catch (e: Exception) {
            Log.e("GEO_DEBUG", "Error en sugerencias: ${e.message}")
        } finally {
            db.close()
        }
        return suggestions.distinct()
    }
}
