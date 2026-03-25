/*package com.tuempresa.chapacollectionapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Chapa::class], version = 8, exportSchema = false)
abstract class ChapaDatabase : RoomDatabase() {
    abstract fun chapaDao(): ChapaDao

    companion object {
        @Volatile
        private var INSTANCE: ChapaDatabase? = null

        // Migración de la versión 3 a la 4: añadimos las columnas de color
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añadimos una columna no nula con valor por defecto para mantener compatibilidad
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN colorPrimario TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN colorSecundario1 TEXT")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN colorSecundario2 TEXT")
            }
        }

        // Migración de la versión 4 a la 5: añadimos columnas de estado
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN estadoForma TEXT")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN estadoRayones TEXT")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN estadoMarcas TEXT")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN estadoOxido TEXT")
                database.execSQL("ALTER TABLE chapa_table ADD COLUMN estadoPercent INTEGER")
            }
        }

        fun getDatabase(context: Context): ChapaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChapaDatabase::class.java,
                    "chapa_database"
                )
                //.addMigrations(MIGRATION_3_4, MIGRATION_4_5) // aplicar migraciones para evitar destrucción de datos
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
    */