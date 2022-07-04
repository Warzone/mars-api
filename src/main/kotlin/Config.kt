package network.warzone.api

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import network.warzone.api.database.models.Broadcast
import network.warzone.api.database.models.JoinSound
import network.warzone.api.database.models.LevelColor
import network.warzone.api.database.models.PunishmentType
import java.io.File
import java.util.*

object Config {
    private val configPath: String? = System.getenv("MARS_CONFIG_PATH")
    private val punTypesPath: String? = System.getenv("MARS_PUNTYPES_PATH")
    private val broadcastsPath: String? = System.getenv("MARS_BROADCASTS_PATH")
    private val levelColorsPath: String? = System.getenv("MARS_LEVEL_COLORS_PATH")
    private val joinSoundsPath: String? = System.getenv("MARS_JOIN_SOUNDS_PATH")

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

    private val joinSoundsRaw: String = if (joinSoundsPath != null) {
        val file = File(joinSoundsPath)
        if (file.exists()) file.readText() else throw RuntimeException("Invalid MARS_LEVEL_COLORS_PATH provided")
    } else getResource("join_sounds.yml")

    var listenPort = 3000
        private set

    var listenHost = "0.0.0.0"
        private set

    var mongoUrl = "mongodb://localhost:27017"
        private set

    var redisHost = "localhost"
        private set

    var apiToken: String =
        System.getenv("MARS_API_TOKEN") ?: throw Exception("MARS_API_TOKEN is a required env variable")
        private set

    var enableIpHashing: Boolean = false
        private set

    var punishmentTypes: List<PunishmentType> = emptyList()
        private set

    var broadcasts: List<Broadcast> = emptyList()
        private set

    var levelColors: List<LevelColor> = emptyList()
        private set

    var joinSounds: List<JoinSound> = emptyList()
        private set

    var punishmentsWebhookUrl: String? = null
        private set

    var reportsWebhookUrl: String? = null
        private set

    init {
        loadConfig()
        loadPunishmentTypes()
        loadBroadcasts()
        loadLevelColors()
        loadJoinSounds()
    }

    private fun loadConfig() {
        if (configFile == null) return
        val config = Properties()
        config.load(configFile.inputStream())

        config.propertyNames().toList().forEach {
            when (it as String) {
                "listen-port" -> this.listenPort = config.getProperty(it).toInt()
                "listen-host" -> this.listenHost = config.getProperty(it)
                "mongo-url" -> this.mongoUrl = config.getProperty(it)
                "redis-host" -> this.redisHost = config.getProperty(it)
                "enable-ip-hashing" -> this.enableIpHashing = config.getProperty(it).toBoolean()
                "webhooks.punishments" -> this.punishmentsWebhookUrl = config.getProperty(it)
                "webhooks.reports" -> this.reportsWebhookUrl = config.getProperty(it)
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

    private fun loadJoinSounds() {
        joinSounds = Yaml.default.decodeFromString(joinSoundsRaw)
    }

    private fun loadLevelColors() {
        val result = Yaml.default.decodeFromString<List<LevelColor>>(levelColorsRaw)
        levelColors = result
    }

    private fun getResource(name: String): String {
        return Config::class.java.classLoader.getResource(name).readText()
    }
}
