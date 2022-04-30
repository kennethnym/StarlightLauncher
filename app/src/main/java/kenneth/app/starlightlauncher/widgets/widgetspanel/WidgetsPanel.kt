package kenneth.app.starlightlauncher.views.widgetspanel

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
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.GESTURE_ACTION_THRESHOLD
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.NOT_HANDLED
import kenneth.app.starlightlauncher.databinding.WidgetsPanelBinding
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.utils.BindingRegister
import kenneth.app.starlightlauncher.api.utils.GestureMover
import kenneth.app.starlightlauncher.utils.activity
import kenneth.app.starlightlauncher.utils.addBackPressedCallback
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

    /**
     * Whether widget edit mode is enabled.
     */
    var isEditModeEnabled = false
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

    private var isDraggingPanel = false

    private var isClick = false

    private var ongoingAnimation: WidgetPanelAnimation? = null

    private val gestureMover = GestureMover().apply {
        targetView = this@WidgetsPanel
    }

    private val binding: WidgetsPanelBinding

    init {
        translationY = appState.halfScreenHeight.toFloat()

        binding = WidgetsPanelBinding.inflate(LayoutInflater.from(context), this, true).also {
            BindingRegister.widgetsPanelBinding = it
        }

        activity?.addBackPressedCallback {
            if (isExpanded && !isEditModeEnabled && !binding.searchBox.hasQueryText) {
                retract()
                HANDLED
            } else NOT_HANDLED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setWindowInsetsAnimationCallback(keyboardAnimation)
        }

        binding.exitEditModeButton.setOnClickListener { exitEditMode() }
    }

    fun unfocusSearchBox() {
        binding.searchBox.unfocus()
    }

    fun showSearchResults() {
        with(binding) {
            widgetList.isVisible = false
            searchResultView.isVisible = true
        }
    }

    fun hideSearchResults() {
        with(binding) {
            searchResultView.isVisible = false
            widgetList.isVisible = true
        }
    }

    fun showWidgets() {
        binding.widgetList.showWidgets()
    }

    fun hideWidgets() {
        binding.widgetList.hideWidgets()
    }

    fun expand() {
        Log.d("starlight", "expand")

        isExpanded = true

        WidgetPanelAnimation(0f)
            .also { ongoingAnimation = it }
            .start()

        with(binding.searchBox) {
            showTopPadding()
            showRetractWidgetPanelButton()
        }

        gestureMover.reset()
    }

    fun retract() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        isExpanded = false

        WidgetPanelAnimation(screenHeight / 2f)
            .also { ongoingAnimation = it }
            .start()

        with(binding.searchBox) {
            removeTopPadding()
            clearFocus()
            showExpandWidgetPanelButton()
        }

        gestureMover.reset()
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

    /**
     * Enable widget editing. Users can reorder and remove widgets.
     */
    fun editWidgets() {
        canBeSwiped = false
        isEditModeEnabled = true
        expand()
        binding.isInEditMode = true
        binding.widgetList.enableDragAndDrop()
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return if (canBeSwiped) handleWidgetPanelGesture(ev)
        else super.onTouchEvent(ev)
    }

    private fun exitEditMode() {
        canBeSwiped = true
        isEditModeEnabled = false
        binding.isInEditMode = false
        binding.widgetList.disableDragAndDrop()
    }

    private fun handleWidgetPanelGesture(ev: MotionEvent?): Boolean =
        when (ev?.actionMasked) {
            MotionEvent.ACTION_BUTTON_PRESS -> performClick()

            MotionEvent.ACTION_DOWN -> {
                initiateGesture(ev)
                HANDLED
            }

            MotionEvent.ACTION_MOVE -> {
                if (gestureMover.isGestureActive) {
                    handleDragGesture(ev)
                } else {
                    initiateGesture(ev)
                }
                HANDLED
            }

            MotionEvent.ACTION_UP -> handleGestureEnd(ev)

            else -> NOT_HANDLED
        }

    private fun handleDragGesture(ev: MotionEvent) {
        Log.d("starlight", "==========")
        Log.d("starlight", "isDraggingPanel $isDraggingPanel")
        Log.d("starlight", "isExpanded $isExpanded")
        Log.d("starlight", "isScrolling $isScrolling")
        if (
            isDraggingPanel ||
            !isExpanded ||
            !isScrolling && ev.rawY - gestureMover.initialY >= 0 && scrollY == 0
        ) {
            Log.d("starlight", "start drag")
            // scroll view is at the top and user wants to swipe down
            // i.e. retract widget panel
            // therefore we let user move the widget panel
            isDraggingPanel = true
            gestureMover.addMotionMoveEvent(ev)
            velocityTracker.addMovement(ev)
        } else {
            Log.d("starlight", "start scroll")
            isDraggingPanel = false
            isScrolling = true
            // let user scroll the content
            super.onTouchEvent(ev)
        }
    }

    private fun handleGestureEnd(ev: MotionEvent): Boolean {
        if (isScrolling) {
            Log.d("starlight", "reset after scrolling")
            gestureMover.reset()
            isScrolling = false
            return super.onTouchEvent(ev)
        }

        isDraggingPanel = false

        with(velocityTracker) {
            addMovement(ev)
            computeCurrentVelocity(1)
        }

        gestureMover.addMotionUpEvent(ev)

        val gestureDistance = gestureMover.gestureDelta
        Log.d("starlight", "gesture distance $gestureDistance, isExpanded $isExpanded")

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

        Log.d("starlight", "reset")
        gestureMover.reset()

        return HANDLED
    }

    private fun initiateGesture(ev: MotionEvent) {
        ongoingAnimation?.cancel()
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

        private var anim: SpringAnimation? = null

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

                    anim = this

                    start()
                }
            }
        }

        fun cancel() {
            anim?.cancel()
        }
    }
}
