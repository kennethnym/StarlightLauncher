package kenneth.app.spotlightlauncher.utils

/**
 * Describes an entry in the floating context menu.
 * Classes that contain information of an entry in the floating context menu should implement
 * this interface.
 */
interface ContextMenuEntry {
    /**
     * The ID of this entry. This is used to uniquely identify this entry when selected.
     */
    val id: Int

    /**
     * The user-facing label of this entry.
     */
    val label: String
}