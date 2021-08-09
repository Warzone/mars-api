package network.warzone.api.database

import com.mongodb.client.result.DeleteResult
import kotlinx.serialization.Serializable
import network.warzone.api.database.model.*
import network.warzone.api.database.model.Map
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.json
import org.litote.kmongo.or
import org.litote.kmongo.reactivestreams.KMongo

object Database {
    val database: CoroutineDatabase;
    val players: CoroutineCollection<Player>
    val sessions: CoroutineCollection<Session>
    val ranks: CoroutineCollection<Rank>
    val tags: CoroutineCollection<Tag>
    val maps: CoroutineCollection<Map>

    init {
        val client = KMongo.createClient().coroutine;
        database = client.getDatabase("warzone-api")
        players = database.getCollection()
        sessions = database.getCollection()
        ranks = database.getCollection()
        tags = database.getCollection()
        maps = database.getCollection()
    }
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findById(id: String): T? {
    return Database.database.getCollection<T>().findOne("{ _id: ${id.json} }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findByName(name: String): T? {
    return Database.database.getCollection<T>().findOne("{ nameLower: ${name.lowercase().json} }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.findByIdOrName(target: String): T? {
    return Database.database.getCollection<T>()
        .findOne("{ \$or: [ { nameLower: ${target.lowercase().json} }, { _id: ${target.json} } ] }")
}

suspend inline fun <reified T : Any> CoroutineCollection<T>.deleteById(id: String): DeleteResult {
    return Database.database.getCollection<T>().deleteOne("{ id: ${id.json} }")
}
