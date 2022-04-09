package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val _id: String,
    val ip: String,
    val player: SimplePlayer,
    val serverId: String,
    val createdAt: Long,
    var endedAt: Long?
) {
    val length: Long?
    get() = if (endedAt == null) null else endedAt!! - createdAt

    fun isActive(): Boolean = endedAt == null
}
