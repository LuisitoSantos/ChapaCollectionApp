package com.tuempresa.chapacollectionapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicProfile(
    val id: String,
    @SerialName("username") val username: String
)
