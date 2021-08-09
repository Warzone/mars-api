package network.warzone.api.http.map

import kotlinx.serialization.Serializable

@Serializable
data class MapLoadOneRequest(val _id: String, val name: String, val version: String, val gamemode: String? = null)
