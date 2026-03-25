package com.tuempresa.chapacollectionapp.components

import androidx.compose.ui.geometry.isEmpty
//import androidx.preference.isNotEmpty
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.ktx.storage
import com.tuempresa.chapacollectionapp.data.Chapa
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("chapa_table")
    private val storage = com.google.firebase.ktx.Firebase.storage
    private val storageRef = storage.reference.child("fotos_chapas")

    fun getChapasFlow(): Flow<List<Chapa>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // toObjects requiere que Chapa tenga un constructor vacío o valores por defecto
                val chapas = snapshot.toObjects(Chapa::class.java)
                trySend(chapas)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun saveChapa(chapa: Chapa, imageUri: android.net.Uri? = null) {
        try {
            var chapaParaGuardar = chapa

            // 1. Si el usuario seleccionó una imagen nueva (URI local)
            if (imageUri != null) {
                val firebaseUrl = uploadImage(imageUri)
                if (firebaseUrl.isNotEmpty()) {
                    // Reemplazamos la ruta local por la URL de internet
                    chapaParaGuardar = chapaParaGuardar.copy(imagePath = firebaseUrl)
                }
            }

            // 2. Gestionar el ID de documento
            val docRef = if (chapaParaGuardar.firestoreId.isEmpty()) {
                collection.document()
            } else {
                collection.document(chapaParaGuardar.firestoreId)
            }

            // 3. Guardar con el ID generado
            val finalChapa = chapaParaGuardar.copy(firestoreId = docRef.id)
            docRef.set(finalChapa).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteChapa(firestoreId: String) {
        if (firestoreId.isNotEmpty()) {
            collection.document(firestoreId).delete().await()
        }
    }

    /**
     * Sube una imagen a Firebase Storage y devuelve su URL pública
     * [localUri] es la ruta del archivo en el móvil
     */
    suspend fun uploadImage(localUri: android.net.Uri): String {
        return try {
            // Creamos un nombre único para la imagen usando el tiempo actual
            val fileName = "chapa_${java.lang.System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(fileName)

            // Subimos el archivo
            imageRef.putFile(localUri).await()

            // Obtenemos la URL de descarga (la que guardaremos en Firestore)
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }
}