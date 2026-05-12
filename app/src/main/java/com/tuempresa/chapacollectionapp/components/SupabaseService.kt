package com.tuempresa.chapacollectionapp.components

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tuempresa.chapacollectionapp.data.Chapa
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SupabaseService {

    // 1. La URL debe ser solo el subdominio de supabase, sin "/chapas"
    private val supabaseUrl = "https://gtdhkepkecrsijkgspjv.supabase.co"
    // Pon aquí tu ANON KEY real (la sacas de Settings -> API en Supabase)
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd0ZGhrZXBrZWNyc2lqa2dzcGp2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU2NjQwMDMsImV4cCI6MjA5MTI0MDAwM30.peTOOVa4k8SBSL_8f1j5aVOmaMwWuQ8FPJpa-0tbhoM"

    private val client = createSupabaseClient(supabaseUrl, supabaseKey) {
        install(Postgrest){
            // ESTA ES LA LÍNEA CLAVE:
            // Configura el serializador para que NO envíe campos nulos
            serializer = KotlinXSerializer(Json {
                encodeDefaults = false // No envía el id si es null
                ignoreUnknownKeys = true
                explicitNulls = false // No envía campos con valor null
            })
        }
        install(Storage)
    }

    suspend fun saveChapa(context: Context, chapa: Chapa, imageUri: Uri?) = withContext(Dispatchers.IO) {
        try {
            var finalImageUrl = chapa.imagePath

            // 2. Subir imagen si existe
            if (imageUri != null) {
                val fileName = "chapa_${System.currentTimeMillis()}.jpg"

                // Acceso al bucket
                val bucket = client.storage.from("fotos_chapas")

                // Leemos los bytes para evitar errores de archivo no encontrado
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.use { it.readBytes() }

                if (bytes != null) {
                    Log.d("Supabase", "Subiendo imagen al bucket: $fileName")

                    // Subida de bytes
                    bucket.upload(fileName, bytes)

                    // 3. Generar la URL pública (Bucket debe ser Public en Supabase)
                    finalImageUrl = "$supabaseUrl/storage/v1/object/public/fotos_chapas/$fileName"
                    Log.d("Supabase", "Imagen subida. URL: $finalImageUrl")
                }
            }

            // 4. Guardar los datos en la tabla 'chapas'
            val chapaFinal = chapa.copy(imagePath = finalImageUrl)

            // Usamos el cliente para insertar en la tabla
            client.from("chapas").insert(chapaFinal)
            Log.d("Supabase", "¡Datos guardados con éxito en la tabla 'chapas'!")

        } catch (e: Exception) {
            Log.e("Supabase", "¡ERROR CRÍTICO!")
            Log.e("Supabase", "Mensaje: ${e.message}")
            Log.e("Supabase", "Causa: ${e.cause}")
            e.printStackTrace() // Esto imprimirá el rastro completo en el Logcat
        }
    }

    suspend fun getChapas(): List<Chapa> = withContext(Dispatchers.IO) {
        try {
            // Trae todas las filas de la tabla 'chapas' y las convierte automáticamente en objetos Chapa
            client.from("chapas").select().decodeList<Chapa>()
        } catch (e: Exception) {
            Log.e("Supabase", "Error al obtener chapas: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteChapa(chapa: Chapa) = withContext(Dispatchers.IO) {
        try {
            // 1. Borrar el registro de la tabla 'chapas'
            client.from("chapas").delete {
                filter {
                    eq("nombre", chapa.nombre)
                }
            }
            Log.d("Supabase", "Registro eliminado de la tabla")

            // 2. Borrar la foto del Storage
            chapa.imagePath?.let { url ->
                // Extraemos el nombre del archivo de la URL
                val fileName = url.substringAfterLast("/")

                // IMPORTANTE: En la versión actual de Supabase-kt se usa .delete()
                // y se le pasa una lista de nombres de archivo
                client.storage.from("fotos_chapas").delete(listOf(fileName))

                Log.d("Supabase", "Archivo $fileName eliminado del Storage")
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Error al borrar chapa: ${e.message}")
        }
    }

    suspend fun updateChapa(context: Context, chapa: Chapa, nuevaImageUri: Uri?) = withContext(Dispatchers.IO) {
        try {
            var chapaFinal = chapa

            // 1. Si hay una imagen nueva
            if (nuevaImageUri != null) {
                // Borramos la antigua primero (usamos la URL que ya venía en el objeto chapa)
                chapa.imagePath?.let { oldUrl ->
                    deleteImageFromStorage(oldUrl)
                }

                // Subimos la nueva
                val newUrl = uploadImage(context, nuevaImageUri)
                if (newUrl != null) {
                    chapaFinal = chapa.copy(imagePath = newUrl)
                }
            }

            // 2. ACTUALIZACIÓN CRÍTICA:
            // Asegúrate de que el ID no sea nulo.
            // En Supabase .update() necesita un filtro para saber QUÉ fila tocar.
            client.from("chapas").update(chapaFinal) {
                filter {
                    // Cambia "id" por el nombre exacto de tu columna en Supabase (suele ser id)
                    eq("id", chapa.id ?: 0)
                }
            }
            Log.d("Supabase", "Chapa con ID ${chapa.id} actualizada correctamente")
        } catch (e: Exception) {
            Log.e("Supabase", "Error al actualizar: ${e.message}")
            throw e
        }
    }


    /*
    suspend fun deleteImageFromStorage(imageUrl: String) {
        try {
            // 1. Limpiar URL de parámetros ?t=...
            val cleanUrl = imageUrl.split("?")[0].trim()
            // 2. Extraer el nombre del archivo
            val fileName = cleanUrl.substringAfterLast("/")

            if (fileName.isNotEmpty() && cleanUrl.contains("supabase")) {
                Log.d("SupabaseStorage", "Intentando borrar de 'fotos_chapas': $fileName")

                // 3. Importante: Asegúrate de que el bucket se llame "fotos_chapas"
                client.storage.from("fotos_chapas").delete(listOf(fileName))

                Log.d("SupabaseStorage", "Borrado solicitado con éxito")
            }
        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Error real al borrar: ${e.message}")
        }
    }

     */

    suspend fun deleteImageFromStorage(imageUrl: String) {
        try {
            // 1. Quitar TODO lo que haya después del "?" (parámetros de caché de Supabase)
            val urlSinParametros = imageUrl.split("?")[0].trim()

            // 2. Extraer el nombre real del archivo
            val fileName = urlSinParametros.substringAfterLast("/")

            if (fileName.isNotEmpty() && urlSinParametros.contains("supabase")) {
                Log.d("SupabaseStorage", "Intentando borrar archivo: $fileName")

                // 3. Importante: Asegurar que se borra del bucket correcto
                client.storage.from("fotos_chapas").delete(listOf(fileName))

                Log.d("SupabaseStorage", "Borrado confirmado en servidor")
            }
        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Error al eliminar: ${e.message}")
        }
    }

    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return try {
            val fileName = "chapa_${System.currentTimeMillis()}.jpg"
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.use { it.readBytes() }

            if (bytes != null) {
                val bucket = client.storage.from("fotos_chapas")
                bucket.upload(fileName, bytes, upsert = true)

                // USAR EL MISMO FORMATO QUE EN SAVECHAPA
                val publicUrl = "$supabaseUrl/storage/v1/object/public/fotos_chapas/$fileName"
                Log.d("SupabaseService", "Nueva imagen subida: $publicUrl")
                publicUrl
            } else null
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error al subir: ${e.message}")
            null
        }
    }
}