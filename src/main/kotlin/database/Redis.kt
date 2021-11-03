package network.warzone.api.database

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams

object Redis {
    val pool: JedisPool = JedisPool(JedisPoolConfig(), "localhost")

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