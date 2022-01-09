package network.warzone.api.util

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import network.warzone.api.Config
import network.warzone.api.http.UnauthorizedException

enum class TokenType(val id: String) {
    BEARER("Bearer"),
    API_TOKEN("API-Token")
}

suspend fun protected(context: PipelineContext<Unit, ApplicationCall>, fn: suspend (serverId: String?) -> Unit) {
    val call = context.call
    val authHeader = call.request.header("Authorization") ?: throw UnauthorizedException()
    val serverIDHeader = call.request.header("Mars-Server-ID")
    val split = authHeader.split(' ')
    if (split.size < 2) throw UnauthorizedException()
    val (tokenType, token) = split
    if (tokenType == TokenType.API_TOKEN.id) { // Request is using an API Token (originating from a trusted source)
        if (serverIDHeader == null) throw UnauthorizedException()
        if (Config.apiToken != token) throw UnauthorizedException()
        fn(serverIDHeader)
    } else if (tokenType == TokenType.BEARER.id) { // Request is using a user token
        throw UnauthorizedException()
    } else throw UnauthorizedException()
}