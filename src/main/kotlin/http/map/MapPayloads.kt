package network.warzone.api.http.map

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.MapContributor
import network.warzone.api.database.models.MapGamemode

@Serializable
data class MapLoadOneRequest(
    val _id: String,
    val name: String,
    val version: String,
    val gamemodes: List<MapGamemode>,
    val authors: List<MapContributor>,
    val contributors: List<MapContributor>
)
