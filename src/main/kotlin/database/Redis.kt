package network.warzone.api.database

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.warzone.api.database.model.Match
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object Redis {
    private val pool: JedisPool = JedisPool(JedisPoolConfig(), "localhost")

    private inline fun <reified T> get(key: String): T? {
        pool.resource.use {
            val value = it.get(key) ?: return null
            return Json.decodeFromString<T>(value)
        }
    }

    private inline fun <reified T> set(key: String, value: T): T {
        pool.resource.use {
            it.set(key, Json.encodeToString(value))
            return value
        }
    }

    fun setCurrentMatch(serverId: String, match: Match) =
        this.set("current_match:${serverId}", match)

    fun getCurrentMatch(serverId: String) = this.get<Match>("current_match:${serverId}")
}