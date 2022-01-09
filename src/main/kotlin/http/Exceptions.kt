package network.warzone.api.http

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class ApiExceptionResponse(val code: String, val message: String, val error: Boolean = true)

open class ApiException(
    val statusCode: HttpStatusCode,
    val type: ApiExceptionType,
    override val message: String = "An error occurred"
) : RuntimeException("${type.name}: $message ($statusCode)") {
    val response: ApiExceptionResponse
        get() {
            return ApiExceptionResponse(type.name, message, error = true)
        }
}

class ValidationException(message: String = "Validation failed") :
    ApiException(HttpStatusCode.BadRequest, ApiExceptionType.VALIDATION_ERROR, message)

class SessionInactiveException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.SESSION_INACTIVE, "The session is not active")

class PlayerMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.PLAYER_MISSING, "The player does not exist")

class RankConflictException :
    ApiException(HttpStatusCode.Conflict, ApiExceptionType.RANK_CONFLICT, "A rank already exists with that name")

class RankMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.RANK_MISSING, "The rank does not exist")

class RankAlreadyPresentException :
    ApiException(
        HttpStatusCode.Conflict,
        ApiExceptionType.RANK_ALREADY_PRESENT,
        "The rank is already present in the list"
    )

class RankNotPresentException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.RANK_NOT_PRESENT, "The rank is not present in the list")

class TagConflictException :
    ApiException(HttpStatusCode.Conflict, ApiExceptionType.TAG_CONFLICT, "A tag already exists with that name")

class TagMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.TAG_MISSING, "The tag does not exist")

class TagAlreadyPresentException :
    ApiException(
        HttpStatusCode.Conflict,
        ApiExceptionType.TAG_ALREADY_PRESENT,
        "The tag is already present in the list"
    )

class TagNotPresentException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.TAG_NOT_PRESENT, "The tag is not present in the list")

class MapMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.MAP_MISSING, "The map does not exist")

class PunishmentMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.PUNISHMENT_MISSING, "The punishment does not exist")

class NoteMissingException :
    ApiException(HttpStatusCode.NotFound, ApiExceptionType.NOTE_MISSING, "The note does not exist")

class InternalServerErrorException : ApiException(
    HttpStatusCode.InternalServerError,
    ApiExceptionType.INTERNAL_SERVER_ERROR,
    "An internal server error occurred. This should not happen. Please report this bug."
)

class UnauthorizedException : ApiException(
    HttpStatusCode.Unauthorized,
    ApiExceptionType.UNAUTHORIZED_EXCEPTION,
    "API credentials are missing or invalid"
)

enum class ApiExceptionType {
    INTERNAL_SERVER_ERROR,
    UNAUTHORIZED_EXCEPTION,
    VALIDATION_ERROR,
    SESSION_INACTIVE,
    PLAYER_MISSING,
    RANK_CONFLICT,
    RANK_MISSING,
    RANK_ALREADY_PRESENT,
    RANK_NOT_PRESENT,
    TAG_CONFLICT,
    TAG_MISSING,
    TAG_ALREADY_PRESENT,
    TAG_NOT_PRESENT,
    MAP_MISSING,
    PUNISHMENT_MISSING,
    NOTE_MISSING
}