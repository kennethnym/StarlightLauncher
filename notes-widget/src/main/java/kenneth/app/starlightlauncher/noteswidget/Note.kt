package kenneth.app.starlightlauncher.noteswidget

import kenneth.app.starlightlauncher.noteswidget.util.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*
import kotlinx.serialization.Serializable

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