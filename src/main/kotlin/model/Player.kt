package network.warzone.api.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import network.warzone.api.util.UUIDSerializer
import java.util.*

@Serializable
data class Player(
    val _id: String,
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val name: String,
    @Contextual val firstJoinedAt: Date,
    @Contextual val lastJoinedAt: Date,
    val playtime: Long,
    val ips: List<String>,
)