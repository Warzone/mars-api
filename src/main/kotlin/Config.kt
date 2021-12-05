package network.warzone.api

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import network.warzone.api.database.models.PunishmentType
import java.io.File
import java.util.*

object Config {
    private val configPath: String? = System.getenv("MARS_CONFIG_PATH")
    private val punTypesPath: String? = System.getenv("MARS_PUNTYPES_PATH")

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
        } else
            Config::class.java.classLoader.getResource("punishment_types.yml").readText()

    var listenPort = 3000
        private set

    var listenHost = "0.0.0.0"
        private set

    var mongoUrl = "mongodb://localhost:27017"
        private set

    var punishmentTypes: List<PunishmentType> = emptyList()
        private set

    init {
        loadConfig()
        loadPunishmentTypes()
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
}