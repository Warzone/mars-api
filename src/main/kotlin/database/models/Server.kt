package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerEvents(var xpMultiplier: XPMultiplier?)

@Serializable
data class XPMultiplier(var value: Float, var player: SimplePlayer? = null, var updatedAt: Long)