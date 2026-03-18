package com.tuempresa.chapacollectionapp.repository

import androidx.annotation.WorkerThread
import com.tuempresa.chapacollectionapp.data.Chapa
import com.tuempresa.chapacollectionapp.data.ChapaDao
import kotlinx.coroutines.flow.Flow

class ChapaRepository(private val chapaDao: ChapaDao) {

    @WorkerThread
    suspend fun insert(chapa: Chapa) {
        chapaDao.insert(chapa)
    }

    @WorkerThread
    suspend fun delete(chapa: Chapa) {
        chapaDao.delete(chapa)
    }

    @WorkerThread
    fun getAllChapas(): Flow<List<Chapa>> {
        return chapaDao.getAllChapas()
    }

    @WorkerThread
    suspend fun update(chapa: Chapa) {
        chapaDao.update(chapa)
    }

    fun getChapaById(id: Int): Flow<Chapa?> {
        return chapaDao.getChapaById(id)
    }
}
