package com.tuempresa.chapacollectionapp.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Chapa::class], version = 2, exportSchema = false)
abstract class ChapaDatabase : RoomDatabase() {

    abstract fun chapaDao(): ChapaDao

    companion object {
        @Volatile
        private var INSTANCE: ChapaDatabase? = null

        fun getDatabase(context: Context): ChapaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChapaDatabase::class.java,
                    "chapa_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
