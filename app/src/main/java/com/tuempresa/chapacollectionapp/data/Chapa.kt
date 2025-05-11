package com.tuempresa.chapacollectionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapa_table")
data class Chapa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val pais: String,
    val imagePath: String? = null
)
