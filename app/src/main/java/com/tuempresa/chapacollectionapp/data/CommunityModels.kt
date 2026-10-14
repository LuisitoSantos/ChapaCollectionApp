package com.tuempresa.chapacollectionapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityProfile(
    val id: String,
    val username: String,
    val country: String? = null,
    @SerialName("chapa_count") val chapaCount: Long = 0
)

@Serializable
data class CommunityChapa(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val username: String,
    val country: String? = null,
    val nombre: String,
    val pais: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("estado_percent") val estadoPercent: Int? = null,
    val etiqueta: String? = null,
    @SerialName("chapa_count") val chapaCount: Long = 0
)
