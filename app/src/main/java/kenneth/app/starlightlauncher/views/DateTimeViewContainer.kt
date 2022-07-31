package kenneth.app.starlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.ANIMATION_FRAME_DELAY
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.util.BindingRegister
import javax.inject.Inject
import kotlin.math.max

/**
 * A simple LinearLayout wrapper that
 * contains DateTimeView and media control widget if there is media currently playing.
 *
 * Contains a layoutWeight getter/setter to enable ObjectAnimator animation.
 */
@AndroidEntryPoint
internal class DateTimeViewContainer(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs) {
    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var launcher: StarlightLauncherApi

    private val choreographer = Choreographer.getInstance()

    init {
        setOnLongClickListener {
            launcher.showOptionMenu { LauncherOptionMenu(context, launcher, it) }
            HANDLED
        }
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

    private fun startAnimation() {
        choreographer.postFrameCallbackDelayed({ scaleSelf(0) }, ANIMATION_FRAME_DELAY)
    }
}