package kenneth.app.spotlightlauncher

/**
 * A true flag that declares something is handled. For example this can be returned
 * by gesture handlers.
 */
const val HANDLED = true

/**
 * A true flag that declares something is not handled. For example this can be returned
 * by gesture handlers.
 */
const val NOT_HANDLED = false

/**
 * Defines how long delay between each choreographer frame should be.
 */
const val ANIMATION_FRAME_DELAY = 1000 / 120L

/**
 * How far should a gesture travel in order to activate certain actions
 */
const val GESTURE_ACTION_THRESHOLD = 100