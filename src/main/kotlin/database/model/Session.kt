package network.warzone.api.database.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Session(
    val _id: String,
    val playerId: String,
    val createdAt: Long,
    var endedAt: Long?
)
