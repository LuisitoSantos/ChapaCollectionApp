package com.tuempresa.chapacollectionapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Chapa::class], version = 3, exportSchema = false)
//@Database(entities = [Chapa::class], version = 1)
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
                ).fallbackToDestructiveMigration() // ¡solo en desarrollo!
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
