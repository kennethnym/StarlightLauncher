package kenneth.app.spotlightlauncher.utils

import javax.inject.Inject

/**
 * Calculates velocity on a one-dimensional axis.
 */
class Velocity1DCalculator @Inject constructor() {
    /**
     * The distance the recorded movement has travelled
     */
    val distance
        get() = finalPoint - initialPoint

    /**
     * Initial point of the movement.
     */
    var initialPoint = 0f
        set(point) {
            initialTimestamp = System.currentTimeMillis()
            field = point
        }

    /**
     * Final point of the movement.
     */
    var finalPoint = 0f
        set(point) {
            field = point
            velocity = (point - initialPoint) / (System.currentTimeMillis() - initialTimestamp)
        }

    /**
     * The velocity of the recorded movement in unit/ms
     */
    var velocity = 0f

    private var initialTimestamp = 0L
}