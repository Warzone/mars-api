package network.warzone.api.util

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import network.warzone.api.Config
import network.warzone.api.database.models.Punishment
import network.warzone.api.database.models.SimplePlayer

object WebhookUtil {
    private const val COLOR_NEW_PUNISHMENT = 0x0077FF
    private const val COLOR_PUNISHMENT_REVERTED = 0x00FF4C
    private const val COLOR_NEW_REPORT = 0xFFEE00

    private var punishmentsClient: WebhookClient? = null
    private var reportsClient: WebhookClient? = null

    init {
        val punishmentsUrl = Config.punishmentsWebhookUrl
        if (punishmentsUrl != null) punishmentsClient = WebhookClient.withUrl(punishmentsUrl)

        val reportsUrl = Config.reportsWebhookUrl
        if (reportsUrl != null) reportsClient = WebhookClient.withUrl(reportsUrl)
    }

    // potential todo: add # of times reported recently
    fun sendReportWebhook(
        serverId: String,
        reporter: SimplePlayer,
        target: SimplePlayer,
        reason: String,
        onlineStaff: Set<SimplePlayer>
    ) {
        reportsClient?.let { client ->
            val embed = WebhookEmbedBuilder()
                .setColor(COLOR_NEW_REPORT)
                .setTitle(WebhookEmbed.EmbedTitle("New report (on $serverId)", null))
                .setThumbnailUrl(target.miniIconUrl())
                .setFooter(WebhookEmbed.EmbedFooter("Reported by ${reporter.name}", reporter.miniIconUrl()))
                .addField(WebhookEmbed.EmbedField(true, "Player", target.name))
                .addField(WebhookEmbed.EmbedField(true, "Reason", reason.escapeMarkdown()))
                .addField(WebhookEmbed.EmbedField(false, "Online staff", onlineStaff.joinToString("\n") { it.name }))

            client.send(embed.build())
        }
    }

    // todo: add relative "expiry" field
    fun sendPunishmentWebhook(punishment: Punishment) {
        punishmentsClient?.let { client ->
            val embed = WebhookEmbedBuilder()
                .setColor(COLOR_NEW_PUNISHMENT)
                .setTitle(WebhookEmbed.EmbedTitle("New punishment", null))
                .setFooter(WebhookEmbed.EmbedFooter("Pun ID: ${punishment._id}", null))
                .setThumbnailUrl(punishment.target.miniIconUrl())
                .addField(WebhookEmbed.EmbedField(true, "Target", punishment.target.name))
                .addField(WebhookEmbed.EmbedField(true, "Staff", punishment.punisher?.name ?: "Console"))
                .addField(WebhookEmbed.EmbedField(true, "Type", punishment.action.kind.verb))
                .addField(WebhookEmbed.EmbedField(false, "Reason", "${punishment.reason.name.escapeMarkdown()} (${punishment.offence})"))

            if (punishment.note != null) embed.addField(WebhookEmbed.EmbedField(true, "Note", punishment.note))

            client.send(embed.build())
        }
    }

    // todo: add relative "issued at" field
    fun sendPunishmentReversionWebhook(punishment: Punishment) {
        punishmentsClient?.let { client ->
            val reversion = punishment.reversion ?: return
            val embed = WebhookEmbedBuilder()
                .setColor(COLOR_PUNISHMENT_REVERTED)
                .setTitle(WebhookEmbed.EmbedTitle("Punishment reverted", null))
                .setFooter(WebhookEmbed.EmbedFooter("Pun ID: ${punishment._id}", null))
                .setThumbnailUrl(punishment.target.miniIconUrl())
                .addField(WebhookEmbed.EmbedField(true, "Target", punishment.target.name))
                .addField(WebhookEmbed.EmbedField(true, "Staff", punishment.punisher?.name ?: "Console"))
                .addField(
                    WebhookEmbed.EmbedField(
                        false,
                        "Punishment",
                        "${punishment.action.kind.verb} - ${punishment.reason.name.escapeMarkdown()} (${punishment.offence})"
                    )
                )
                .addField(WebhookEmbed.EmbedField(true, "Reversion reason", reversion.reason.escapeMarkdown()))

            client.send(embed.build())
        }
    }

    private fun escapeMarkdown(string: String, htmlMode: Boolean = false): String {
        var escaped = string
            .replace("*", "\\*")
            .replace("/", "\\/")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("_", "\\_")

        if (htmlMode) escaped = string
            .replace("#", "\\#")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        return escaped
    }

    private fun String.escapeMarkdown() = WebhookUtil.escapeMarkdown(this, false)

    private fun SimplePlayer.miniIconUrl(): String {
        return "https://crafatar.com/avatars/$id?helm&size=50"
    }
}