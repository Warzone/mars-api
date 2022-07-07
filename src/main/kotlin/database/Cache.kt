package network.warzone.api.database

import org.litote.kmongo.json
import redis.clients.jedis.params.SetParams

abstract class Cache(val resourceName: String, val lifetimeMs: Long? = null) {
    suspend inline fun <reified T : Any> get(_key: String): T? {
        val key = _key.lowercase()
        val cachedValue = Redis.get<T>("$resourceName:$key")
        if (cachedValue != null) return cachedValue

        return Database.database.getCollection<T>()
            .findOne("{ \$or: [ { nameLower: ${key.lowercase().json} }, { _id: ${key.json} } ] }")
    }

    suspend inline fun <reified T : Any> set(
        _key: String,
        value: T,
        persist: Boolean = false,
        options: SetParams? = if (lifetimeMs != null) SetParams().px(lifetimeMs) else SetParams()
    ) {
        val key = _key.lowercase()
//        val name =
//            "${Thread.currentThread().stackTrace[1].className}-${Thread.currentThread().stackTrace[1].methodName}"
        Redis.set("$resourceName:$key", value, options)
        if (persist) Database.database.getCollection<T>().save(value)
    }

    suspend inline fun <reified T : Any> persist(_key: String) {
        val key = _key.lowercase()
        val value: T? = get(key)
        if (value != null) Database.database.getCollection<T>().save(value)
    }
}

// Cached match expires after one day
const val MATCH_LIFETIME = 86_400_000L // MS

// Player expires after three hours
const val PLAYER_LIFETIME = 10_800_000L // MS

// Punishment protection expires after ten seconds
const val PLAYER_PROTECTION_LIFETIME = 10_000L // MS

object PlayerCache : Cache("player", PLAYER_LIFETIME)
object MatchCache : Cache("match", MATCH_LIFETIME)

object PunishmentProtectionCache : Cache("playerPunishProtect", PLAYER_PROTECTION_LIFETIME)
