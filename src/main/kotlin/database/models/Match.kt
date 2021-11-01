package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
//    var firstBlood: FirstBlood?
)

@Serializable
data class FirstBlood(var attacker: SimplePlayer, var victim: SimplePlayer, var date: Long)