package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val _id: String,
    val playerId: String,
    val createdAt: Long,
    var endedAt: Long?
)
