package com.tuempresa.chapacollectionapp.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//Para Firebase
@Serializable
data class Chapa @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("id")
    @EncodeDefault // Evita enviar el campo si es su valor por defecto (null)
    val id: Long? = null,
    val nombre: String = "",
    val pais: String = "",
    val ciudad: String? = null,
    val imagePath: String? = null,
    val anio: Int? = null,
    // Nuevos campos de color
    val colorPrimario: String = "",
    val colorSecundario1: String? = null,
    val colorSecundario2: String? = null,
    // Campos de estado
    val estadoForma: String? = null,
    val estadoRayones: String? = null,
    val estadoMarcas: String? = null,
    val estadoOxido: String? = null,
    val estadoPercent: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val procedencia: String? = null,
    val metodoObtencion: String? = null,
    val donante: String? = null,
    val paisObtencion: String? = null,
    val ciudadObtencion: String? = null,
    @SerialName("user_id") val userId: String? = null
) {

}
