package com.tuempresa.chapacollectionapp.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapaDao {

    @Insert
    suspend fun insert(chapa: Chapa)

    @Delete
    suspend fun delete(chapa: Chapa)

    @Query("SELECT * FROM chapa_table ORDER BY id ASC")
    fun getAllChapas(): Flow<List<Chapa>>

    @Update
    suspend fun update(chapa: Chapa)

    @Query("SELECT * FROM chapa_table WHERE id = :id")
    fun getChapaById(id: Int): Flow<Chapa?> // <--- AÑADE ESTA LÍNEA
}
