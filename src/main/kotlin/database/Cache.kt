package network.warzone.api.database

import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Player
import org.litote.kmongo.json
import redis.clients.jedis.params.SetParams

open class Cache(val resourceName: String) {
    suspend inline fun <reified T : Any> get(key: String): T? {
        val cachedValue = Redis.get<T>("$resourceName:$key")
        if (cachedValue !== null) return cachedValue

        return Database.database.getCollection<T>()
            .findOne("{ \$or: [ { nameLower: ${key.lowercase().json} }, { _id: ${key.json} } ] }")
    }

    inline fun <reified T : Any> set(key: String, value: T, options: SetParams? = SetParams()) {
        Redis.set("$resourceName:$key", value, options)
    }
}

object PlayerCache : Cache("player")
object MatchCache : Cache("match")