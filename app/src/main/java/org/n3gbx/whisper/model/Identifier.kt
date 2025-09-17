package org.n3gbx.whisper.model

import kotlinx.serialization.Serializable

@Serializable
data class Identifier(
    val localId: String,
    val externalId: String
)
