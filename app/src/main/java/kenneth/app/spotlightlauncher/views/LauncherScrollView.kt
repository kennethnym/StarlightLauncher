package kenneth.app.spotlightlauncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.AppState_Factory
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val SCROLL_DIRECTION_DOWN = 1

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
@AndroidEntryPoint
class LauncherScrollView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    @Inject
    lateinit var appState: AppState

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (!canScrollVertically(SCROLL_DIRECTION_DOWN) && !appState.isSearchBoxActive) {
            when (ev?.actionMasked) {
                null -> {
                }
                MotionEvent.ACTION_UP -> {
                    launcherOptionMenu?.let {
                        isDragging = false

                        val gestureDuration =
                            System.currentTimeMillis() - initialGestureDownTimestamp
                        val deltaY = ev.y - initialY
                        val gestureVelocity = deltaY / gestureDuration

                        when {
                            deltaY >= -1 && deltaY <= 1 -> {
                                // user attempted to click the shade of the menu
                                // hide the menu

                                if (it.isActive) {
                                    it.hide()
                                }
                            }
                            gestureVelocity > 2 -> {
                                it.hide()
                            }
                            gestureVelocity < -2 -> {
                                it.show()
                            }
                            else -> {
                                // gesture too slow
                                // revert menu back to its position

                                if (it.isActive) {
                                    // launcherOptionMenu is currently active
                                    it.show()
                                } else {
                                    it.hide()
                                }
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

                            // when scrolled to bottom, all gestures are intercepted, including
                            // when the user wants to scroll back up.
                            // we have to make sure to allow the user to scroll back up
                            // when the menu is not active.
                            //
                            // if the menu is active or when the user is swiping up,
                            // count them as interactions with the menu.
                            // otherwise, let the scroll view handle the gesture.

                            if (it.isActive || delta < 0) {
                                it.translationY =
                                    min(it.height.toFloat(), max(0f, it.translationY + delta * 2f))
                                prevY = ev.y
                            } else {
                                return super.onTouchEvent(ev)
                            }
                        }
                    }
                }
            }

            return true
        }

        return super.onTouchEvent(ev)
    }
}