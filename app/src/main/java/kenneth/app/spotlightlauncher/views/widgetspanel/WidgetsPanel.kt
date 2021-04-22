package kenneth.app.spotlightlauncher.views.widgetspanel

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.*
import kenneth.app.spotlightlauncher.databinding.WidgetsPanelBinding
import kenneth.app.spotlightlauncher.utils.GestureMover
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.utils.addBackPressedCallback
import kenneth.app.spotlightlauncher.views.LauncherOptionMenu
import java.util.*
import javax.inject.Inject

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
@AndroidEntryPoint
class WidgetsPanel(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    @Inject
    lateinit var appState: AppState

    private lateinit var velocityTracker: VelocityTracker

    private var isVelocityTrackerObtained = false

    private var isScrolling = false

    private var isPanelDragged = false

    private var isExpanded = false

    private val gestureMover = GestureMover().apply {
        targetView = this@WidgetsPanel
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

    private val binding: WidgetsPanelBinding

    init {
        isFillViewport = true
        fitsSystemWindows = false
        translationY = appState.halfScreenHeight.toFloat()

        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        binding = WidgetsPanelBinding.inflate(LayoutInflater.from(context), this, true)

        activity?.addBackPressedCallback {
            if (!binding.overlay.isVisible && isExpanded) {
                retract()
                HANDLED
            } else NOT_HANDLED
        }
    }

    override fun onTouchEvent(ev: MotionEvent?) =
        handleWidgetPanelGesture(ev)

    fun showSearchResults() {
        binding.widgetList.isVisible = false
        binding.searchResultContainer.isVisible = true
    }

    fun hideSearchResults() {
        binding.searchResultContainer.isVisible = false
        binding.widgetList.isVisible = true
    }

    fun showWidgets() {
        binding.widgetList.showWidgets()
    }

    fun hideWidgets() {
        binding.widgetList.hideWidgets()
    }

    fun expand() {
        Log.d("hub", "expand")
        isExpanded = true
        WidgetPanelAnimation(0f).start()
        binding.searchBox.showTopPadding()
    }

    fun retract() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        isExpanded = false
        WidgetPanelAnimation(screenHeight / 2f).start()
        with(binding.searchBox) {
            removeTopPadding()
            clearFocus()
        }
    }

    fun showOverlayFrom(view: View, contentConstructor: (context: Context) -> View) {
        binding.overlay.run {
            showFrom(view, withContent = contentConstructor(context))
        }
    }

    private fun handleWidgetPanelGesture(ev: MotionEvent?): Boolean {
        return when (ev?.actionMasked) {
            null -> NOT_HANDLED

            MotionEvent.ACTION_UP -> handleGestureEnd(ev)

            MotionEvent.ACTION_DOWN -> {
                initiateGesture(ev)
                HANDLED
            }

            MotionEvent.ACTION_MOVE ->
                if (gestureMover.isGestureActive) {
                    handleDragGesture(ev)
                } else {
                    initiateGesture(ev)
                    HANDLED
                }

            else -> NOT_HANDLED
        }
    }

    private fun handleDragGesture(ev: MotionEvent): Boolean =
        if (
            isPanelDragged ||
            !isExpanded ||
            !isScrolling && ev.y - gestureMover.initialY > 0 && scrollY == 0
        ) {
            // scroll view is at the top and user wants to swipe down
            // i.e. retract widget panel
            // therefore we let user move the widget panel
            isPanelDragged = true
            gestureMover.addMotionMoveEvent(ev)
            velocityTracker.addMovement(ev)

            HANDLED
        } else {
            isScrolling = true
            // let user scroll the content
            super.onTouchEvent(ev)
        }

    private fun handleGestureEnd(ev: MotionEvent): Boolean {
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
                expand()
            }
            gestureDistance > GESTURE_ACTION_THRESHOLD -> {
                retract()
            }
            // revert to original position because gesture is not fast enough
            isExpanded -> expand()
            !isExpanded -> retract()
        }

        gestureMover.reset()

        return HANDLED
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
            if (this@WidgetsPanel.translationY != finalPosition) {
                SpringAnimation(
                    this@WidgetsPanel,
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
}
