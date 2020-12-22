package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.activity
import kotlin.math.max
import kotlin.math.min

private const val SCROLL_DIRECTION_DOWN = 1

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
class LauncherScrollView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    /**
     * The initial y position that initiated the swipe up gesture.
     */
    private var initialY: Float = 0f

    /**
     * The previous y position of the swipe up gesture.
     */
    private var prevY: Float = 0f

    /**
     * Determines if the option menu is being dragged.
     */
    private var isDragging = false

    /**
     * Records the initial time when the user begins the drag gesture.
     */
    private var initialGestureDownTimestamp = 0L

    private var launcherOptionMenu: LauncherOptionMenu? = null

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            activity?.let {
                launcherOptionMenu = it.findViewById(R.id.launcher_option_menu)
            }
        }
    }

    init {
        isFillViewport = true
        fitsSystemWindows = false

        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    /**
     * Determines if touch event should be intercepted.
     * In any of the following cases, it will be intercepted:
     *   - when the scroll view has reached the bottom. we want to bring the launcher option menu
     *     when the user keeps swiping up.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return when (ev?.actionMasked) {
            MotionEvent.ACTION_UP -> isDragging
            MotionEvent.ACTION_MOVE -> !canScrollVertically(SCROLL_DIRECTION_DOWN)
            else -> false
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked) {
            null -> {
            }
            MotionEvent.ACTION_BUTTON_PRESS -> {
                if (launcherOptionMenu?.isActive == true) {
                    launcherOptionMenu?.hide()
                }
                isDragging = false
                performClick()
            }
            MotionEvent.ACTION_UP -> {
                launcherOptionMenu?.let {
                    isDragging = false

                    val gestureDuration = System.currentTimeMillis() - initialGestureDownTimestamp
                    val gestureVelocity = (ev.y - initialY) / gestureDuration

                    if (gestureVelocity > 5) {
                        it.hide()
                    } else if (gestureVelocity < -5) {
                        it.show()
                    } else {
                        if (it.isActive) {
                            // launcherOptionMenu is currently active
                            it.show()
                        } else {
                            it.hide()
                        }
                    }
                }
            }
            else -> {
                launcherOptionMenu?.let {
                    if (!isDragging) {
                        isDragging = true
                        initialGestureDownTimestamp = System.currentTimeMillis()
                        initialY = ev.y
                        prevY = initialY
                        it.isVisible = true
                    } else {
                        val delta = ev.y - prevY
                        it.translationY =
                            min(it.height.toFloat(), max(0f, it.translationY + delta * 2f))
                        prevY = ev.y
                    }
                }
            }
        }

        return super.onTouchEvent(ev)
    }
}