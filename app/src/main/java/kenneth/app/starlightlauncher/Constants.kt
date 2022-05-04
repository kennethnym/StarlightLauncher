package kenneth.app.starlightlauncher

/**
 * A true flag that declares something is handled. For example this can be returned
 * by gesture handlers.
 */
internal const val HANDLED = true

/**
 * A false flag that declares something is not handled. For example this can be returned
 * by gesture handlers.
 */
internal const val NOT_HANDLED = false

/**
 * Defines how long delay between each choreographer frame should be.
 */
internal const val ANIMATION_FRAME_DELAY = 1000 / 120L

/**
 * How far should a gesture travel in order to activate certain actions
 */
internal const val GESTURE_ACTION_THRESHOLD = 100