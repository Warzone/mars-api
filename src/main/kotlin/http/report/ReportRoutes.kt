package network.warzone.api.http.report

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import network.warzone.api.util.WebhookUtil
import network.warzone.api.util.protected
import network.warzone.api.util.validate

/**
 * In-game reports (so far)
 * Used to forward reports to external integration (e.g. Discord webhook)
 */
fun Route.manageReports() {
    post {
        protected(this) { serverId ->
            validate<ReportCreateRequest>(this) { data ->
                WebhookUtil.sendReportWebhook(
                    serverId ?: "Unknown",
                    data.reporter,
                    data.target,
                    data.reason,
                    data.onlineStaff
                )
                call.respond(Unit)
            }
        }
    }
}

fun Application.reportRoutes() {
    routing {
        route("/mc/reports") {
            manageReports()
        }
    }
}