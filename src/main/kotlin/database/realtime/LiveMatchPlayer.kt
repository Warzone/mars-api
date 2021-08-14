package network.warzone.api.database.realtime

import kotlinx.serialization.Serializable

@Serializable
data class LiveMatchPlayer(val name: String, val id: String, var partyName: String?) {
    override fun equals(other: Any?): Boolean {
        if (other is LiveMatchPlayer) return other.id == this.id
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}