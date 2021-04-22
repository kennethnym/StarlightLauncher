package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.ANIMATION_FRAME_DELAY
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject
import kotlin.math.max

/**
 * A simple LinearLayout wrapper that
 * contains DateTimeView and media control widget if there is media currently playing.
 *
 * Contains a layoutWeight getter/setter to enable ObjectAnimator animation.
 */
@AndroidEntryPoint
class DateTimeViewContainer(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var choreographer: Choreographer

    var layoutWeight: Float
        get() = (layoutParams as LayoutParams).weight
        set(newWeight) {
            val newLayoutParams = (layoutParams as LayoutParams).apply {
                weight = newWeight
            }
            layoutParams = newLayoutParams
        }

    init {
        setOnLongClickListener {
            BindingRegister.activityMainBinding.launcherOptionMenu.show()
            HANDLED
        }
    }

    /**
     * Scales itself based on where the widget panel is.
     */
    private fun scaleSelf(delay: Long) {
        val widgetsPanel = BindingRegister.activityMainBinding.widgetsPanel

        val dateTimeViewScale = max(
            0f,
            (y - widgetsPanel.y) / (y - appState.halfScreenHeight)
        )

        scaleX = dateTimeViewScale
        scaleY = dateTimeViewScale

        startAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.INVISIBLE || visibility == View.GONE) {
            choreographer.removeFrameCallback(::scaleSelf)
        } else {
            startAnimation()
        }
    }

    private fun startAnimation() {
        choreographer.postFrameCallbackDelayed(::scaleSelf, ANIMATION_FRAME_DELAY)
    }
}