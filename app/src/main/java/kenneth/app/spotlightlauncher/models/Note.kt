package kenneth.app.spotlightlauncher.models

import kotlinx.serialization.Serializable

/**
 * Represents a note created by the user.
 */
@Serializable
data class Note(val content: String, val dueDateTimestamp: Int?)
