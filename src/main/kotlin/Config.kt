package network.warzone.api

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import network.warzone.api.database.models.Broadcast
import network.warzone.api.database.models.LevelColor
import network.warzone.api.database.models.PunishmentType
import java.io.File
import java.util.*

object Config {
    private val configPath: String? = System.getenv("MARS_CONFIG_PATH")
    private val punTypesPath: String? = System.getenv("MARS_PUNTYPES_PATH")
    private val broadcastsPath: String? = System.getenv("MARS_BROADCASTS_PATH")
    private val levelColorsPath: String? = System.getenv("MARS_LEVEL_COLORS_PATH")

    private val configFile: File? =
        if (configPath != null) {
            val file = File(configPath)
            if (file.exists()) file else throw RuntimeException("Invalid MARS_CONFIG_PATH provided")
        } else
            null

    private val punTypesRaw: String =
        if (punTypesPath != null) {
            val file = File(punTypesPath)
            if (file.exists()) file.readText() else throw RuntimeException("Invalid MARS_PUNTYPES_PATH provided")
        } else getResource("punishment_types.yml")

    private val broadcastsRaw: String = if (broadcastsPath != null) {
        val file = File(broadcastsPath)
        if (file.exists()) file.readText() else throw RuntimeException("Invalid MARS_BROADCASTS_PATH provided")
    } else getResource("broadcasts.yml")

    private val levelColorsRaw: String = if (levelColorsPath != null) {
        val file = File(levelColorsPath)
        if (file.exists()) file.readText() else throw RuntimeException("Invalid MARS_LEVEL_COLORS_PATH provided")
    } else getResource("level_colors.yml")

    var listenPort = 3000
        private set

    var listenHost = "0.0.0.0"
        private set

    var mongoUrl = "mongodb://localhost:27017"
        private set

    var punishmentTypes: List<PunishmentType> = emptyList()
        private set

    var broadcasts: List<Broadcast> = emptyList()
        private set

    var levelColors: List<LevelColor> = emptyList()
        private set

    init {
        loadConfig()
        loadPunishmentTypes()
        loadBroadcasts()
        loadLevelColors()
    }

    private fun loadConfig() {
        if (configFile == null) return
        val config = Properties()
        config.load(configFile.inputStream())

        config.propertyNames().toList().forEach {
            when (it) {
                "listenPort" -> this.listenPort = config.getProperty("listenPort").toInt()
                "listenHost" -> this.listenHost = config.getProperty("listenHost")
                "mongoUrl" -> this.mongoUrl = config.getProperty("mongoUrl")
            }
        }
    }

    private fun loadPunishmentTypes() {
        val result = Yaml.default.decodeFromString<List<PunishmentType>>(punTypesRaw)
        punishmentTypes = result
    }

    private fun loadBroadcasts() {
        val result = Yaml.default.decodeFromString<List<Broadcast>>(broadcastsRaw)
        broadcasts = result
    }

    private fun loadLevelColors() {
        val result = Yaml.default.decodeFromString<List<LevelColor>>(levelColorsRaw)
        levelColors = result
    }

    private fun getResource(name: String): String {
        return Config::class.java.classLoader.getResource(name).readText()
    }
}
