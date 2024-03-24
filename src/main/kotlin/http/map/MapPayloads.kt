package network.warzone.api.http.map

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.LevelContributor
import network.warzone.api.database.models.LevelGamemode

@Serializable
data class MapLoadOneRequest(
    val _id: String,
    val name: String,
    val version: String,
    val gamemodes: List<LevelGamemode>,
    val authors: List<LevelContributor>,
    val contributors: List<LevelContributor>
)