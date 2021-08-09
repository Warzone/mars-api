package network.warzone.api.http.map

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.database.model.Map
import network.warzone.api.util.validate

fun Route.manageMaps() {
    post {
        validate<List<MapLoadOneRequest>>(this) { mapList ->
            val mapsToSave = mutableListOf<Map>()
            mapList.forEach { map ->
                val existingMap = Database.maps.findById(map._id)
                if (existingMap !== null) { // Updating existing map (e.g. new version)
                    existingMap.name = map.name
                    existingMap.nameLower = existingMap.name.lowercase()
                    existingMap.version = map.version
                    existingMap.gamemode = map.gamemode
                    mapsToSave.add(existingMap)
                } else { // Map is new
                    mapsToSave.add(
                        Map(
                            _id = map._id,
                            name = map.name,
                            nameLower = map.name.lowercase(),
                            version = map.version,
                            gamemode = map.gamemode,
                            loadedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            mapsToSave.forEach { Database.maps.save(it) }

            val maps = Database.maps.find().toList()
            call.respond(maps)
        }
    }

    get {
        val maps = Database.maps.find().toList()
        call.respond(maps)
    }
}

fun Application.mapRoutes() {
    routing {
        route("/mc/maps") {
            manageMaps()
        }
    }
}