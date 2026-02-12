package com.tuempresa.chapacollectionapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapa_table")
data class Chapa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val pais: String,
    val imagePath: String? = null,
    val anio: Int? = null,
    val colorPrimario: String = "",
    val colorSecundario1: String? = null,
    val colorSecundario2: String? = null,
    val estadoForma: String? = null,
    val estadoRayones: String? = null,
    val estadoMarcas: String? = null,
    val estadoOxido: String? = null,
    val estadoPercent: Int? = null
)
