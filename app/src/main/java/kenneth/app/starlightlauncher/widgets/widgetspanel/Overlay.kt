package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.view.Plate
import kenneth.app.starlightlauncher.util.activity
import javax.inject.Inject

/**
 * Defines, in milliseconds, how long the animation of showing [Overlay] should be.
 */
private const val SHOW_OVERLAY_ANIMATION_DURATION = 200L

private val SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR =
    PathInterpolator(0.33f, 1f, 0.68f, 1f)

/**
 * An empty view that overlays on top of [WidgetsPanel].
 * Widgets can use this to display additional info.
 */
@AndroidEntryPoint
internal class Overlay(context: Context, attrs: AttributeSet) : Plate(context, attrs) {
    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var blurHandler: BlurHandler

    /**
     * The [View] that this [Overlay] expanded from.
     */
    private lateinit var originalView: View

    /**
     * Whether the closing animation of [Overlay] is being played.
     */
    private var isClosing = false

    /**
     * The content of [Overlay]
     */
    private var content: View? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isVisible && !isClosing) {
                close()
            }
        }
    }

    /**
     * Shows this [Overlay] by animating from [view]
     */
    fun showFrom(view: View, withContent: View) {
        isVisible = true
        originalView = view
        content = withContent

        blurWith(blurHandler)

        translationY = appState.screenHeight.toFloat()

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, appState.screenHeight.toFloat(), 0f)

        val opacityAnimator = ObjectAnimator.ofFloat(content, View.ALPHA, 1f)

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR
            playTogether(yAnimator, opacityAnimator)
            start()
        }

        displayContent(withContent)
        activity?.let {
            it.onBackPressedDispatcher.addCallback(it, onBackPressedCallback)
        }
    }

    fun close() {
        isClosing = true

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, appState.screenHeight.toFloat())
        val opacityAnimator = ObjectAnimator.ofFloat(content, View.ALPHA, 0f)

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR

            playTogether(yAnimator, opacityAnimator)
            addListener({
                isVisible = false
                isClosing = false
            })

            start()
        }

        onBackPressedCallback.remove()
    }

    private fun displayContent(content: View) {
        if (childCount > 1) {
            removeViewAt(1)
        }
        addView(content)
    }
}