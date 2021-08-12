package network.warzone.api.socket.listeners

import kotlinx.serialization.json.JsonObject
import network.warzone.api.socket.MinecraftConnection
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener

object LogListener : SocketListener() {
    override suspend fun handle(conn: MinecraftConnection, event: SocketEvent, json: JsonObject) {
        println("INCOMING SOCKET EVENT (From ${conn.serverInfo.id}) | Event: $event | Data: $json")
    }
}