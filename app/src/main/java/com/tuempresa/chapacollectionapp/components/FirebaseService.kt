package com.tuempresa.chapacollectionapp.components

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tuempresa.chapacollectionapp.data.Chapa
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("chapa_table")
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("fotos_chapas")

    fun getChapasFlow(): Flow<List<Chapa>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseService", "Error en el listener de Firestore: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val chapas = snapshot.toObjects(Chapa::class.java)
                trySend(chapas)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun saveChapa(chapa: Chapa, imageUri: Uri? = null) {
        try {
            var chapaParaGuardar = chapa

            if (imageUri != null) {
                val urlNube = uploadImage(imageUri)
                if (urlNube.isNotEmpty()) {
                    // AQUÍ: Sobreescribimos el null inicial con la URL real de internet
                    chapaParaGuardar = chapaParaGuardar.copy(imagePath = urlNube)
                }
            }

            val docId = if (chapaParaGuardar.id?.equals(null) == true) {
                collection.document().id
            } else {
                chapaParaGuardar.id
            }

            //val finalChapa = chapaParaGuardar.copy(id = docId)
            val finalChapa = null
            //collection.document(docId.toString()).set(finalChapa).await()
            //Log.d("FirebaseService", "Documento guardado en Firestore con imagePath: ${finalChapa.imagePath}")

        } catch (e: Exception) {
            Log.e("FirebaseService", "Error en saveChapa: ${e.message}")
        }
    }

    private suspend fun uploadImage(localUri: Uri): String {
        return try {
            val fileName = "chapa_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(fileName)

            Log.d("FirebaseService", "Abriendo stream de datos para la Uri: $localUri")

            // 1. Obtenemos el contexto de la aplicación para poder leer la Uri
            val context = com.google.firebase.FirebaseApp.getInstance().applicationContext

            // 2. Abrimos el flujo de datos del archivo (esto salta las restricciones de file://)
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: throw Exception("No se pudo abrir el flujo de datos de la imagen")

            // 3. Subimos el Stream (el chorro de bytes) en lugar del archivo físico
            imageRef.putStream(inputStream).await()

            // 4. Cerramos el stream para liberar memoria
            inputStream.close()

            // 5. Obtenemos la URL de descarga definitiva
            val downloadUrl = imageRef.downloadUrl.await()
            Log.d("FirebaseService", "¡Subida exitosa! URL: $downloadUrl")

            downloadUrl.toString()

        } catch (e: Exception) {
            Log.e("FirebaseService", "Error detallado en uploadImage: ${e.message}")
            "" // Devolvemos vacío si falla para que el imagePath sea null y no rompa la app
        }
    }

    suspend fun deleteChapa(firestoreId: String) {
        try {
            if (!firestoreId.isNullOrEmpty()) {
                collection.document(firestoreId).delete().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al eliminar de Firestore: ${e.message}")
        }
    }
}