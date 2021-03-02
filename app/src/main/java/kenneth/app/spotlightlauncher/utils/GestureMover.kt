package kenneth.app.spotlightlauncher.utils

import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.lang.IllegalArgumentException
import kotlin.math.max

/**
 * [GestureMover] moves [GestureMover.targetView] vertically according to the recorded motion events.
 */
class GestureMover {
    lateinit var targetView: View

    var gestureDelta = 0f

    var isGestureActive = false
        private set

    /**
     * The minimum y position [GestureMover.targetView] should have.
     *
     */
    var minY: Float? = null

    private var initialY = 0f

    private var viewInitialY = 0f

    /**
     * Records the initial motion event of the gesture
     */
    fun recordInitialEvent(event: MotionEvent) {
        initialY = event.y
        isGestureActive = true
        viewInitialY = targetView.y
        Log.d("hub", "initial y $viewInitialY")
    }

    /**
     * Records the ongoing [MotionEvent] of the gesture
     */
    fun addMotionMoveEvent(event: MotionEvent) {
        Log.d("hub", "y ${event.y}")

        if (event.actionMasked != MotionEvent.ACTION_MOVE)
            throw IllegalArgumentException("The given motion event must be ACTION_MOVE")

        val delta = event.y - initialY
        val minY = this.minY

        Log.d("hub", "translation ${targetView.translationY}")

        if (minY != null) {
            targetView.translationY = max(targetView.translationY + delta, minY)
        } else {
            targetView.translationY += delta
        }
    }

    /**
     * Records the final [MotionEvent] of the gesture
     */
    fun addMotionUpEvent(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_UP)
            throw IllegalArgumentException("The given motion event must be ACTION_UP")

        Log.d("hub", "view final y ${targetView.y}")
        gestureDelta = targetView.y - viewInitialY
        isGestureActive = false
    }

    /**
     * Resets all parameters.
     */
    fun reset() {
        isGestureActive = false
        initialY = 0f
        viewInitialY = 0f
        gestureDelta = 0f
        minY = null
    }
}