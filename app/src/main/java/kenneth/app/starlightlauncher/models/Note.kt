package kenneth.app.starlightlauncher.models

import kenneth.app.starlightlauncher.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

/**
 * Represents a note created by the user.
 */
@Serializable
data class Note(
    val content: String,

    val id: String = UUID.randomUUID().toString(),

    @Serializable(with = LocalDateTimeSerializer::class)
    val dueOn: LocalDateTime? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    val createdOn: LocalDateTime = LocalDateTime.now(),
)
