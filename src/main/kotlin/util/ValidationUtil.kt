package network.warzone.api.util

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import network.warzone.api.http.ValidationException
import org.valiktor.Constraint
import org.valiktor.ConstraintViolationException
import org.valiktor.Validator
import java.util.*

val PLAYER_NAME_REGEX = Regex("^[a-zA-Z0-9_]{1,16}\$")
val IP_V4_REGEX = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")

suspend inline fun <reified T : Any> validate(context: PipelineContext<Unit, ApplicationCall>, fn: (data: T) -> Unit) {
    try {
        val data = context.call.receive<T>()
        fn(data)
    } catch (ex: ConstraintViolationException) {
        val violation = ex.constraintViolations.first()
        throw ValidationException("Validation failed for '${violation.property}' (value: ${violation.value})")
    } catch (ex: Exception) {
        throw ValidationException("Validation failed! Ensure there are no extra properties in the JSON body")
    }
}

object PlayerName : Constraint

fun <E> Validator<E>.Property<String?>.isPlayerName() = this.validate(PlayerName) {
    if (it == null) return@validate false
    return@validate it.matches(PLAYER_NAME_REGEX)
}

object IPv4 : Constraint

fun <E> Validator<E>.Property<String?>.isIPv4() = this.validate(IPv4) {
    if (it == null) return@validate false
    return@validate it.matches(IP_V4_REGEX)
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}
