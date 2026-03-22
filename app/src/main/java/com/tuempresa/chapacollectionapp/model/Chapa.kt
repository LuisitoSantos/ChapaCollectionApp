package com.tuempresa.chapacollectionapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapa_table")
data class Chapa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val pais: String,
    val ciudad: String? = null,
    val imagePath: String? = null,
    val anio: Int? = null,
    val colorPrimario: String = "",
    val colorSecundario1: String? = null,
    val colorSecundario2: String? = null,
    val estadoForma: String? = null,
    val estadoRayones: String? = null,
    val estadoMarcas: String? = null,
    val estadoOxido: String? = null,
    val estadoPercent: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val procedencia: String? = null ,
    val metodoObtencion: String? = null,
    val donante: String? = null,
    val paisObtencion: String? = null,
    val ciudadObtencion: String? = null
)
