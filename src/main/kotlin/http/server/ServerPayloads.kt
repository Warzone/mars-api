package network.warzone.api.http.server

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.SimplePlayer
import network.warzone.api.database.models.XPMultiplier
import java.util.*

@Serializable
data class ServerStatusResponse(val lastAliveTime: Long, val currentMatch: Match, val statsTracking: Boolean)

@Serializable
data class XPMultiplierRequest(var value: Float, var player: SimplePlayer? = null) {
    fun toXPMultiplier(): XPMultiplier {
        return XPMultiplier(value, player, Date().time)
    }
}