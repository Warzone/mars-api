package network.warzone.api.socket.listeners

import kotlinx.serialization.json.JsonObject
import network.warzone.api.database.realtime.LiveMinecraftServer
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener

object LogListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        println("INCOMING SOCKET EVENT (From ${server.id}) | Event: $event | Data: $json")
    }
}