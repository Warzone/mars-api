package network.warzone.api.socket.listeners

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.realtime.*
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import network.warzone.api.socket.format

object PartyListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.PARTY_JOIN -> onMemberAdd(server, format.decodeFromJsonElement(json))
            SocketEvent.PARTY_LEAVE -> onMemberRemove(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onMemberAdd(server: LiveMinecraftServer, data: PartyMemberAddData) {
        val current =
            server.currentMatch ?: return println("Joining party in non-existent match? ${data.playerName}")
        val party = current.parties.find { it.name == data.partyName }
            ?: return println("Party not found ${data.partyName}")
        val player = current.participants[data.playerId]
        if (player == null) { // Player is new to the match
            current.participants[data.playerId] = LiveMatchPlayer(data.playerName, data.playerId, party.name)
        } else {
            player.partyName = party.name
            current.participants[player.id] = player
        }
        current.events.add(PartyMemberAddEvent(data))
        current.save()
    }

    private fun onMemberRemove(server: LiveMinecraftServer, data: PartyMemberRemoveData) {
        val current =
            server.currentMatch ?: return println("Leaving party in non-existent match? ${data.playerName}")
        val player = current.participants[data.playerId]
            ?: return println("Player leaving party when they were never in one: ${data.playerName}")
        player.partyName = null
        current.participants[data.playerId] = player
        current.events.add(PartyMemberRemoveEvent(data))
        current.save()
    }
}

@Serializable
data class PartyMemberAddData(val playerId: String, val playerName: String, val partyName: String)

@Serializable
data class PartyMemberRemoveData(val playerId: String, val playerName: String)