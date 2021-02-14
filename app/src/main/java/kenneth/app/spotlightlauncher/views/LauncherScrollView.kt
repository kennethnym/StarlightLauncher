package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.*
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.Velocity1DCalculator
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * The amount of pixel the user has to move in order to expand/retract widget panel.
 * Defined to be 150px.
 */
private const val WIDGET_PANEL_MOVE_THRESHOLD = 150

/**
 * Main NestedScrollView on the home screen. Handles home screen scrolling logic.
 */
@AndroidEntryPoint
class LauncherScrollView(context: Context, attrs: AttributeSet) : NestedScrollView(context, attrs) {
    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var velocityCalculator: Velocity1DCalculator

    /**
     * The previous y position of gesture.
     */
    private var prevY: Float = 0f

    /**
     * Determines if the option menu is being dragged.
     */
    private var isDragging = false

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

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return handleWidgetPanelGesture(ev)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun expandWidgetPanel() {
        appState.isWidgetPanelExpanded = true
        WidgetPanelAnimation(0f).start()
        DateTimeViewAnimation(0f).start()
        BindingRegister.activityMainBinding.searchBox.showTopPadding()
    }

    fun retractWidgetPanel() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        appState.isWidgetPanelExpanded = false
        WidgetPanelAnimation(screenHeight / 2f).start()
        DateTimeViewAnimation(1f).start()
        with(BindingRegister.activityMainBinding.searchBox) {
            removeTopPadding()
            clearFocus()
        }
    }

    private fun handleWidgetPanelGesture(ev: MotionEvent?): Boolean {
        return when (ev?.actionMasked) {
            null -> NOT_HANDLED
            MotionEvent.ACTION_UP -> {
                isDragging = false
                velocityCalculator.finalPoint = y
                val gestureDelta = velocityCalculator.distance

                Log.d("hub", "gesture velocity ${velocityCalculator.velocity}")

                when {
                    gestureDelta < -WIDGET_PANEL_MOVE_THRESHOLD -> {
                        expandWidgetPanel()
                    }
                    gestureDelta > WIDGET_PANEL_MOVE_THRESHOLD -> {
                        retractWidgetPanel()
                    }
                    !appState.isWidgetPanelExpanded -> retractWidgetPanel()
                    appState.isWidgetPanelExpanded -> expandWidgetPanel()
                }

                HANDLED
            }
            MotionEvent.ACTION_DOWN -> {
                recordGestureStart(ev)
                HANDLED
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    recordGestureStart(ev)
                    HANDLED
                } else {
                    val dateTimeViewY = BindingRegister.activityMainBinding.dateTimeView.y
                    // the delta y between activity_main > date_time_view and this view
                    // if this is positive, this view is below date_time_view. vice versa.
                    val delta = ev.y - prevY
                    val scrollViewTranslationY =
                        BindingRegister.activityMainBinding.pageScrollView.translationY

                    with(BindingRegister.activityMainBinding) {
                        pageScrollView.translationY = scrollViewTranslationY + delta

                        val dateTimeViewScale = max(
                            0f,
                            (dateTimeViewY - y) / (dateTimeViewY - appState.halfScreenHeight)
                        )

                        dateTimeView.apply {
                            scaleX = dateTimeViewScale
                            scaleY = dateTimeViewScale
                        }
                    }

                    HANDLED
                }
            }
            else -> NOT_HANDLED
        }
    }

    private fun recordGestureStart(ev: MotionEvent) {
        prevY = ev.y
        isDragging = true
        velocityCalculator.initialPoint = y
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

                if (velocityCalculator.velocity > 0) {
                    setStartVelocity(velocityCalculator.velocity)
                }

                start()
            }
        }
    }

    private inner class DateTimeViewAnimation(private val finalScale: Float) {
        private val springDamping = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        private val springStiffness = SpringForce.STIFFNESS_MEDIUM

        fun start() {
            SpringAnimation(
                BindingRegister.activityMainBinding.dateTimeView,
                DynamicAnimation.SCALE_X,
                finalScale
            ).run {
                spring.apply {
                    dampingRatio = springDamping
                    stiffness = springStiffness
                }

                if (velocityCalculator.velocity > 0) {
                    setStartVelocity(velocityCalculator.velocity)
                }

                start()
            }

            SpringAnimation(
                BindingRegister.activityMainBinding.dateTimeView,
                DynamicAnimation.SCALE_Y,
                finalScale
            ).run {
                spring.apply {
                    dampingRatio = springDamping
                    stiffness = springStiffness
                }

                if (velocityCalculator.velocity > 0) {
                    setStartVelocity(velocityCalculator.velocity)
                }

                start()
            }
        }
    }
}
