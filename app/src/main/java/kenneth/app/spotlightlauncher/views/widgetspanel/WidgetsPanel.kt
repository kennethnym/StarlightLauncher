package kenneth.app.spotlightlauncher.views.widgetspanel

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.*
import kenneth.app.spotlightlauncher.databinding.WidgetsPanelBinding
import kenneth.app.spotlightlauncher.searching.Searcher
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
    /**
     * Determines whether [WidgetsPanel] can be expanded/retracted with swipes.
     */
    var canBeSwiped = true

    /**
     * Whether [WidgetsPanel] is expanded
     */
    var isExpanded = false
        private set

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var searcher: Searcher

    private lateinit var velocityTracker: VelocityTracker

    @RequiresApi(Build.VERSION_CODES.Q)
    private val keyboardAnimation = KeyboardAnimation(this, appState)

    private var isVelocityTrackerObtained = false

    private var isScrolling = false

    private var isPanelDragged = false

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
        translationY = appState.halfScreenHeight.toFloat()

        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        binding = WidgetsPanelBinding.inflate(LayoutInflater.from(context), this, true)

        activity?.addBackPressedCallback {
            if (!binding.overlay.isVisible && isExpanded && !binding.searchBox.hasQueryText) {
                retract()
                HANDLED
            } else NOT_HANDLED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setWindowInsetsAnimationCallback(keyboardAnimation)
        }

        searcher.addSearchResultListener {
            if (searcher.hasFinishedSearching) {
                activity?.runOnUiThread {
                    binding.searchBox.shouldShowLoadingIndicator = false
                }
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent?) =
        if (canBeSwiped) handleWidgetPanelGesture(ev)
        else super.onTouchEvent(ev)

    fun unfocusSearchBox() {
        binding.searchBox.unfocus()
    }

    fun showSearchResults() {
        with(binding) {
            widgetList.isVisible = false
            searchResultContainer.isVisible = true
            searchBox.shouldShowLoadingIndicator = true
        }
    }

    fun hideSearchResults() {
        with(binding) {
            searchResultContainer.isVisible = false
            widgetList.isVisible = true
            searchBox.shouldShowLoadingIndicator = false
        }
    }

    fun showWidgets() {
        binding.widgetList.showWidgets()
    }

    fun hideWidgets() {
        binding.widgetList.hideWidgets()
    }

    fun expand() {
        isExpanded = true
        WidgetPanelAnimation(0f).start()
        with(binding.searchBox) {
            showTopPadding()
            showRetractWidgetPanelButton()
        }
        gestureMover.reset()
    }

    fun retract() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        isExpanded = false
        WidgetPanelAnimation(screenHeight / 2f).start()
        with(binding.searchBox) {
            removeTopPadding()
            clearFocus()
            showExpandWidgetPanelButton()
        }
        gestureMover.reset()
    }

    fun showOverlayFrom(view: View, contentConstructor: (context: Context) -> View) {
        binding.overlay.showFrom(view, withContent = contentConstructor(context))
    }

    /**
     * Sets the currently focused view so that [WidgetsPanel]
     * can move appropriately to avoid the onscreen keyboard from blocking the view.
     */
    fun avoidView(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            keyboardAnimation.avoidView(view)
        }
    }

    /**
     * Opposite of [avoidView]. [WidgetsPanel] will not move to avoid the keyboard.
     */
    fun stopAvoidingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            keyboardAnimation.stopAvoidingView()
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

    private fun handleDragGesture(ev: MotionEvent): Boolean {
        Log.d(
            "hub",
            "isScrolling $isScrolling, isPanelDragged $isPanelDragged, isExpanded $isExpanded "
                + "${ev.y} " + " ${gestureMover.initialY}"
        )
        return if (
            isPanelDragged ||
            !isExpanded ||
            !isScrolling && ev.y - gestureMover.initialY >= 0 && scrollY == 0
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
    }

    private fun handleGestureEnd(ev: MotionEvent): Boolean {
        if (isScrolling) {
            Log.d("hub", "no logner scrolling")
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
