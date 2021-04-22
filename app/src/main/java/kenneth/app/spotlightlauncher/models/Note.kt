package kenneth.app.spotlightlauncher.models

import kenneth.app.spotlightlauncher.utils.InstantSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a note created by the user.
 */
@Serializable
data class Note(
    val content: String,

    @Serializable(with = InstantSerializer::class)
    val dueOn: Instant? = null
) {
    @Serializable(with = InstantSerializer::class)
    val createdOn: Instant = Clock.System.now()
}
