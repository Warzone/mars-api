package network.warzone.api.socket

import network.warzone.api.database.realtime.LiveMinecraftServer
import java.util.*
import kotlin.collections.LinkedHashSet

data class MinecraftServerInfo(val id: String, val token: String)

object ConnectionStore {
    val minecraftServers: MutableSet<LiveMinecraftServer> = Collections.synchronizedSet(LinkedHashSet())
}