package kenneth.app.starlightlauncher.views.widgetspanel

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.animation.addListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.NOT_HANDLED
import kenneth.app.starlightlauncher.animations.DimensionAnimatable
import kenneth.app.starlightlauncher.animations.DimensionAnimator
import kenneth.app.starlightlauncher.api.view.Plate
import kenneth.app.starlightlauncher.utils.BindingRegister
import kenneth.app.starlightlauncher.utils.mainActivity
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
class Overlay(context: Context, attrs: AttributeSet) : Plate(context, attrs),
    DimensionAnimatable by DimensionAnimator() {
    @Inject
    lateinit var appState: AppState

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

    init {
        targetView = this
        mainActivity?.addBackPressListener {
            if (isVisible && !isClosing) {
                close()
                HANDLED
            } else NOT_HANDLED
        }
    }

    /**
     * Shows this [Overlay] by animating from [view]
     */
    fun showFrom(view: View, withContent: View) {
        isVisible = true
        originalView = view
        content = withContent

        val offsetRect = Rect().apply {
            view.getDrawingRect(this)
            BindingRegister.activityMainBinding.widgetsPanel
                .offsetDescendantRectToMyCoords(view, this)
        }

        translationX = offsetRect.left.toFloat()
        translationY = offsetRect.top.toFloat()

        val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)
            .getInsets(WindowInsetsCompat.Type.systemBars())

        val xAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_X, offsetRect.left.toFloat(), 0f)

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offsetRect.top.toFloat(), 0f)

        val opacityAnimator = ObjectAnimator.ofFloat(content, View.ALPHA, 1f)

        val widthAnimator = ObjectAnimator.ofInt(this, "width", view.width, appState.screenWidth)
        val heightAnimator =
            ObjectAnimator.ofInt(
                this,
                "height",
                view.height,
                appState.screenHeight + insets.top
            )

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR
            playTogether(widthAnimator, heightAnimator, xAnimator, yAnimator, opacityAnimator)
            start()
        }

        displayContent(withContent)
    }

    private fun displayContent(content: View) {
        if (childCount > 1) {
            removeViewAt(1)
        }
        addView(content)
    }

    private fun close() {
        isClosing = true

        val offsetRect = Rect().apply {
            originalView.getDrawingRect(this)
            BindingRegister.activityMainBinding.widgetsPanel
                .offsetDescendantRectToMyCoords(originalView, this)
        }

        val xAnimator = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, offsetRect.left.toFloat())
        val yAnimator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offsetRect.top.toFloat())
        val opacityAnimator = ObjectAnimator.ofFloat(content, View.ALPHA, 0f)

        val widthAnimator = ObjectAnimator.ofInt(this, "width", originalView.width)
        val heightAnimator = ObjectAnimator.ofInt(this, "height", originalView.height)

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR

            playTogether(widthAnimator, heightAnimator, xAnimator, yAnimator, opacityAnimator)
            addListener({
                isVisible = false
                isClosing = false
            })

            start()
        }
    }
}