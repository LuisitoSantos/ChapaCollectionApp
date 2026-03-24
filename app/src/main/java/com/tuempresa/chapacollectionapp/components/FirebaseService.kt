package com.tuempresa.chapacollectionapp.components

// ELIMINADO: import androidx.compose.ui.geometry.isEmpty <--- ESTO CAUSABA EL ERROR
import androidx.compose.ui.geometry.isEmpty
import com.google.firebase.firestore.FirebaseFirestore
import com.tuempresa.chapacollectionapp.data.Chapa
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("chapas")

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

    suspend fun saveChapa(chapa: Chapa) {
        try {
            // Ahora .isEmpty() funcionará correctamente sobre el String firestoreId
            val docRef = if (chapa.firestoreId.isEmpty()) {
                collection.document()
            } else {
                collection.document(chapa.firestoreId)
            }

            val finalChapa = chapa.copy(firestoreId = docRef.id)
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
}