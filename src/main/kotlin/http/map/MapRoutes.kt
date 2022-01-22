package network.warzone.api.http.map

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.database.models.Level
import network.warzone.api.database.models.LevelRecords
import network.warzone.api.http.MapMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import java.util.*

fun Route.manageMaps() {
    post {
        protected(this) { _ ->
            validate<List<MapLoadOneRequest>>(this) { mapList ->
                val now = Date().time
                val mapsToSave = mutableListOf<Level>()
                mapList.forEach { map ->
                    println(map)
                    val existingMap = Database.levels.findById(map._id)
                    if (existingMap !== null) { // Updating existing map (e.g. new version)
                        existingMap.name = map.name
                        existingMap.nameLower = existingMap.name.lowercase()
                        existingMap.version = map.version
                        existingMap.gamemodes = map.gamemodes
                        existingMap.authors = map.authors
                        existingMap.updatedAt = now
                        existingMap.contributors = map.contributors
                        mapsToSave.add(existingMap)
                    } else { // Map is new
                        mapsToSave.add(
                            Level(
                                _id = map._id,
                                name = map.name,
                                nameLower = map.name.lowercase(),
                                version = map.version,
                                gamemodes = map.gamemodes,
                                loadedAt = now,
                                updatedAt = now,
                                authors = map.authors,
                                contributors = map.contributors,
                                records = LevelRecords()
                            )
                        )
                    }
                }
                mapsToSave.forEach { Database.levels.save(it) }

                val maps = Database.levels.find().toList()
                call.respond(maps)
            }
        }
    }

    get {
        val maps = Database.levels.find().toList()
        call.respond(maps)
    }

    get("/{mapId}") {
        val mapId = call.parameters["mapId"] ?: throw ValidationException()
        val map = Database.levels.findById(mapId) ?: throw MapMissingException()
        call.respond(map)
    }
}

fun Application.mapRoutes() {
    routing {
        route("/mc/maps") {
            manageMaps()
        }
    }
}