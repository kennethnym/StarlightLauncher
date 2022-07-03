package kenneth.app.starlightlauncher.util

import android.content.ComponentName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A [KSerializer] that serializes [LocalDateTime]
 */
internal object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(decoder.decodeLong()), ZoneId.systemDefault())

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeLong(value.atZone(ZoneId.systemDefault()).toEpochSecond())
}

/**
 * A [KSerializer] that serializes [ComponentName]
 */
internal object ComponentNameSerializer : KSerializer<ComponentName> {
    override val descriptor = PrimitiveSerialDescriptor("Flattened", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ComponentName =
        ComponentName.unflattenFromString(decoder.decodeString())!!

    override fun serialize(encoder: Encoder, value: ComponentName) =
        encoder.encodeString(value.flattenToString())
}
