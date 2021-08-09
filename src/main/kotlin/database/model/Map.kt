package network.warzone.api.database.model

import kotlinx.serialization.Serializable

@Serializable
data class Map(
    val _id: String,
    var name: String,
    var nameLower: String,
    var version: String,
    var gamemode: String?,
    val loadedAt: Long
    // todo: author list
)
