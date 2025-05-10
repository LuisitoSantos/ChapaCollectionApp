package com.tuempresa.chapacollectionapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    //@Insert
    suspend fun insert(chapa: Chapa)

    @Delete
    suspend fun delete(chapa: Chapa)

    @Query("SELECT * FROM chapa_table")
    fun getAllChapas(): Flow<List<Chapa>>

    @Update
    suspend fun update(chapa: Chapa)
}
