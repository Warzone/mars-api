package network.warzone.api.database

import network.warzone.api.database.model.Player
import network.warzone.api.database.model.Rank
import network.warzone.api.database.model.Session
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object Database {
    private val database: CoroutineDatabase;
    val players: CoroutineCollection<Player>
    val sessions: CoroutineCollection<Session>
    val ranks: CoroutineCollection<Rank>

    init {
        val client = KMongo.createClient().coroutine;
        database = client.getDatabase("warzone-api")
        players = database.getCollection()
        sessions = database.getCollection()
        ranks = database.getCollection()
    }
}