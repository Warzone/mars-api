package network.warzone.api.http

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiExceptionResponse(val code: String, val message: String, val error: Boolean = true)

open class ApiException(
    val statusCode: HttpStatusCode,
    val type: ApiExceptionType,
    override val message: String = "An error occurred"
) : RuntimeException("${type.code}: $message ($statusCode)")

class ValidationException(message: String = "Validation failed") :
    ApiException(HttpStatusCode.BadRequest, ApiExceptionType.VALIDATION_ERROR, message)

class SessionInactiveException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.SESSION_INACTIVE, "The session is not active")

class PlayerMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.PLAYER_MISSING, "The player does not exist")

enum class ApiExceptionType(val code: String) {
    VALIDATION_ERROR("VALIDATION_ERROR"),
    SESSION_INACTIVE("SESSION_INACTIVE"),
    PLAYER_MISSING("PLAYER_MISSING");
}