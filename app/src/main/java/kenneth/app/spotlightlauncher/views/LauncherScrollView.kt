package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.*
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.GestureMover
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
@AndroidEntryPoint
class LauncherScrollView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    @Inject
    lateinit var appState: AppState

    private lateinit var velocityTracker: VelocityTracker

    private var isVelocityTrackerObtained = false

    private var isScrolling = false

    private var isPanelDragged = false

    private val gestureMover = GestureMover().apply {
        targetView = this@LauncherScrollView
    }

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

        translationY = appState.halfScreenHeight.toFloat()

        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun onTouchEvent(ev: MotionEvent?) =
        handleWidgetPanelGesture(ev)

    fun expandWidgetPanel() {
        Log.d("hub", "expand")
        appState.isWidgetPanelExpanded = true
        WidgetPanelAnimation(0f).start()
        BindingRegister.activityMainBinding.searchBox.showTopPadding()
    }

    fun retractWidgetPanel() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        appState.isWidgetPanelExpanded = false
        WidgetPanelAnimation(screenHeight / 2f).start()
        with(BindingRegister.activityMainBinding.searchBox) {
            removeTopPadding()
            clearFocus()
        }
    }

    private fun handleWidgetPanelGesture(ev: MotionEvent?): Boolean {
        return when (ev?.actionMasked) {
            null -> NOT_HANDLED
            MotionEvent.ACTION_UP -> {
                if (isScrolling) {
                    isScrolling = false
                    return super.onTouchEvent(ev)
                }

                isPanelDragged = false

                with(velocityTracker) {
                    addMovement(ev)
                    computeCurrentVelocity(1)
                }

                gestureMover.addMotionUpEvent(ev)

                val gestureDistance = gestureMover.gestureDelta

                when {
                    gestureDistance < -GESTURE_ACTION_THRESHOLD -> {
                        expandWidgetPanel()
                    }
                    gestureDistance > GESTURE_ACTION_THRESHOLD -> {
                        retractWidgetPanel()
                    }
                    // revert to original position because gesture is not fast enough
                    appState.isWidgetPanelExpanded -> expandWidgetPanel()
                    !appState.isWidgetPanelExpanded -> retractWidgetPanel()
                }

                gestureMover.reset()

                HANDLED
            }
            MotionEvent.ACTION_DOWN -> {
                initiateGesture(ev)
                HANDLED
            }
            MotionEvent.ACTION_MOVE -> {
                if (gestureMover.isGestureActive) {
                    if (
                        isPanelDragged ||
                        !appState.isWidgetPanelExpanded ||
                        !isScrolling && ev.y - gestureMover.initialY > 0 && scrollY == 0
                    ) {
                        // scroll view is at the top and user wants to swipe down
                        // i.e. retract widget panel
                        // therefore we let user move the widget panel
                        isPanelDragged = true
                        gestureMover.addMotionMoveEvent(ev)
                        velocityTracker.addMovement(ev)
                    } else if (!isPanelDragged) {
                        isScrolling = true
                        // let user scroll the content
                        return super.onTouchEvent(ev)
                    }

                } else {
                    initiateGesture(ev)
                }
                HANDLED
            }
            else -> NOT_HANDLED
        }
    }

    private fun initiateGesture(ev: MotionEvent) {
        gestureMover.recordInitialEvent(ev)
        obtainVelocityTracker().also {
            it.addMovement(ev)
        }
    }

    private fun obtainVelocityTracker(): VelocityTracker {
        velocityTracker = VelocityTracker.obtain()
        isVelocityTrackerObtained = true
        return velocityTracker
    }

    private inner class WidgetPanelAnimation(private val finalPosition: Float) {
        private val springDamping = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        private val springStiffness = SpringForce.STIFFNESS_MEDIUM

        fun start() {
            SpringAnimation(
                this@LauncherScrollView,
                DynamicAnimation.TRANSLATION_Y,
                finalPosition
            ).run {
                spring.apply {
                    dampingRatio = springDamping
                    stiffness = springStiffness
                }

                if (isVelocityTrackerObtained && velocityTracker.yVelocity > 0) {
                    setStartVelocity(velocityTracker.yVelocity)
                    velocityTracker.recycle()
                } else {
                    setStartVelocity(1f)
                }

                start()
            }
        }
    }
}
