package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.*
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.GESTURE_ACTION_THRESHOLD
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.NOT_HANDLED
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.GestureMover
import kenneth.app.starlightlauncher.databinding.WidgetsPanelBinding
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.api.util.activity
import java.lang.Integer.max
import javax.inject.Inject

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
@AndroidEntryPoint
internal class WidgetsPanel(context: Context, attrs: AttributeSet) :
    NestedScrollView(context, attrs),
    OnApplyWindowInsetsListener,
    ViewTreeObserver.OnGlobalFocusChangeListener {
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

    @Inject
    lateinit var launcher: StarlightLauncherApi

    @Inject
    lateinit var bindingRegister: BindingRegister

    private var velocityTracker: VelocityTracker? = null

    private var isVelocityTrackerObtained = false

    private var isScrolling = false

    private var isDraggingPanel = false

    private var isClick = false

    private var ongoingAnimation: WidgetPanelAnimation? = null

    private var originalTranslationY: Float? = null

    private var focusedView: View? = null

    private val gestureMover = GestureMover().apply {
        targetView = this@WidgetsPanel
    }

    private val binding: WidgetsPanelBinding

    private val onBackPressedCallback: OnBackPressedCallback

    private val keyboardAnimation = KeyboardAnimation()

    init {
        translationY = appState.halfScreenHeight.toFloat()

        binding = WidgetsPanelBinding.inflate(LayoutInflater.from(context), this, true).also {
            bindingRegister.widgetsPanelBinding = it
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isExpanded && !isEditModeEnabled) {
                    if (binding.searchBox.hasQueryText) {
                        binding.searchBox.clear()
                    } else {
                        retract()
                    }
                }
            }
        }

        ViewCompat.setWindowInsetsAnimationCallback(this, keyboardAnimation)
        ViewCompat.setOnApplyWindowInsetsListener(this, this)
        viewTreeObserver.addOnGlobalFocusChangeListener(this)
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
        isExpanded = true

        WidgetPanelAnimation(0f)
            .also { ongoingAnimation = it }
            .start()

        with(binding.searchBox) {
            showTopPadding()
            showRetractWidgetPanelButton()
        }

        gestureMover.reset()
        activity?.let {
            it.onBackPressedDispatcher.addCallback(it, onBackPressedCallback)
        }
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
        onBackPressedCallback.remove()
    }

    /**
     * Enables widget editing. Users can reorder and remove widgets.
     */
    fun editWidgets() {
        canBeSwiped = false
        isEditModeEnabled = true
        expand()
        binding.isInEditMode = true
        binding.widgetList.enableDragAndDrop()
    }

    /**
     * Disables widget editing.
     */
    fun exitEditMode() {
        canBeSwiped = true
        isEditModeEnabled = false
        binding.isInEditMode = false
        with(binding.widgetList) {
            exitEditMode()
            disableDragAndDrop()
        }
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val focusedView = this.focusedView ?: return insets
        if (focusedView.id == R.id.search_box_edit_text) return insets

        val y = IntArray(2).run {
            focusedView.getLocationOnScreen(this)
            this[1] + focusedView.height
        }
        val imeHeight = insets
            .getInsets(WindowInsetsCompat.Type.ime())
            .bottom
        // y coordinate of the top of ime
        val imeY = appState.screenHeight - imeHeight

        if (imeHeight > 0) {
            originalTranslationY = translationY
            translationY -= max(0, y + 100 - imeY)
        } else {
            originalTranslationY?.let { translationY = it }
        }

        return insets
    }

    override fun onGlobalFocusChanged(prevFocusedView: View?, focusedView: View?) {
        this.focusedView = focusedView
        keyboardAnimation.shouldAnimate =
            focusedView != null && focusedView.id != R.id.search_box_edit_text
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (canBeSwiped) handleWidgetPanelGesture(ev)
        else super.onTouchEvent(ev)
    }

    private fun handleWidgetPanelGesture(ev: MotionEvent): Boolean =
        when (ev.actionMasked) {
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
        val velocityTracker = this.velocityTracker
        when {
            velocityTracker == null -> super.onTouchEvent(ev)

            isDraggingPanel || !isExpanded ||
                    !isScrolling && ev.rawY - gestureMover.initialY >= 0 && scrollY == 0 -> {
                // scroll view is at the top and user wants to swipe down
                // i.e. retract widget panel
                // therefore we let user move the widget panel
                isDraggingPanel = true
                gestureMover.addMotionMoveEvent(ev)
                velocityTracker.addMovement(ev)
            }

            else -> {
                isDraggingPanel = false
                isScrolling = true
                // let user scroll the content
                super.onTouchEvent(ev)
            }
        }
    }

    private fun handleGestureEnd(ev: MotionEvent): Boolean {
        val velocityTracker = this.velocityTracker ?: return super.onTouchEvent(ev)

        if (isScrolling) {
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

        velocityTracker.recycle()
        this.velocityTracker = null
        gestureMover.reset()

        return HANDLED
    }

    private fun initiateGesture(ev: MotionEvent) {
        isClick = true
        ongoingAnimation?.cancel()
        gestureMover.recordInitialEvent(ev)
        obtainVelocityTracker().also {
            it.addMovement(ev)
        }
    }

    private fun obtainVelocityTracker(): VelocityTracker {
        val velocityTracker = VelocityTracker.obtain()
        this.velocityTracker = velocityTracker
        isVelocityTrackerObtained = true
        return velocityTracker
    }

    private inner class WidgetPanelAnimation(private val finalPosition: Float) {
        private val springDamping = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        private val springStiffness = SpringForce.STIFFNESS_MEDIUM

        private var anim: SpringAnimation? = null

        fun start() {
            val velocityTracker = VelocityTracker.obtain()
            if (this@WidgetsPanel.translationY != finalPosition && velocityTracker != null) {
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

    private inner class KeyboardAnimation :
        WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        var shouldAnimate = true

        private var startTranslationY: Float? = null
        private var endTranslationY: Float? = null

        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
            if (shouldAnimate) {
                startTranslationY = translationY
                super.onPrepare(animation)
            }
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            if (shouldAnimate) {
                endTranslationY = translationY
                originalTranslationY?.let { translationY = it }
            }
            return super.onStart(animation, bounds)
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
            if (!shouldAnimate) return insets
            val startTranslationY = this.startTranslationY ?: return insets
            val endTranslationY = this.endTranslationY ?: return insets

            // Find an IME animation.
            val imeAnimation = runningAnimations.find {
                it.typeMask and WindowInsetsCompat.Type.ime() != 0
            } ?: return insets

            val delta = startTranslationY - endTranslationY

            // Offset the view based on the interpolated fraction of the IME animation.
            translationY = startTranslationY - (delta * imeAnimation.interpolatedFraction)

            return insets
        }
    }
}
