package network.warzone.api.database

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.warzone.api.database.models.Player
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams

object Cache {
    val pool: JedisPool = JedisPool(JedisPoolConfig(), "localhost")

    /*
    * todo: generalise cache methods for different resources?
    * Find player by name from memory cache (Redis)
    * Will fetch from database if player is not cached
    * skipCache? - immediately fetches from DB (false)
    * cacheUpdate? - should the player be added to the cache if not previously present? (true)
    */
    suspend fun findPlayerByName(name: String, skipCache: Boolean = false, cacheUpdate: Boolean = true): Player? {
        if (skipCache) {
            val player = Database.players.findByName(name) ?: return null
            if (cacheUpdate) set(name, player)
            return player
        }

        var player = get<Player>(name)
        return if (player != null) player
        else {
            player = Database.players.findByName(name)
            if (player != null) {
                if (cacheUpdate) set(name, player)
                player
            } else
                return null
        }
    }

    inline fun <reified T> get(key: String): T? {
        pool.resource.use {
            val value = it.get(key) ?: return null
            return Json.decodeFromString<T>(value)
        }
    }

    inline fun <reified T> set(key: String, value: T, setParams: SetParams? = SetParams()) {
        pool.resource.use {
            it.set(key, Json.encodeToString(value), setParams)
        }
    }
}