package network.warzone.api.socket.connection

import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

data class MinecraftServerInfo(val id: String, val token: String)
class MinecraftConnection(val serverInfo: MinecraftServerInfo, val session: DefaultWebSocketServerSession)

object ConnectionStore {
    val minecraftConnections: MutableSet<MinecraftConnection> = Collections.synchronizedSet(LinkedHashSet())
}