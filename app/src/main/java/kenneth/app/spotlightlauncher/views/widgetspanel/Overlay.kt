package kenneth.app.spotlightlauncher.views.widgetspanel

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.animation.addListener
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.NOT_HANDLED
import kenneth.app.spotlightlauncher.animations.DimensionAnimatable
import kenneth.app.spotlightlauncher.animations.DimensionAnimator
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.utils.addBackPressedCallback
import kenneth.app.spotlightlauncher.utils.mainActivity
import kenneth.app.spotlightlauncher.views.BlurView

/**
 * An empty view that overlays on top of [WidgetsPanel].
 * Widgets can use this to display additional info.
 */
class Overlay(context: Context, attrs: AttributeSet) :
    BlurView(context, attrs),
    DimensionAnimatable by DimensionAnimator() {
    /**
     * The [View] that this [Overlay] expanded from.
     */
    private lateinit var originalView: View

    /**
     * Whether the closing animation of [Overlay] is being played.
     */
    private var isClosing = false

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
        startBlur()

        val offsetRect = Rect().apply {
            view.getDrawingRect(this)
            BindingRegister.activityMainBinding.widgetsPanel
                .offsetDescendantRectToMyCoords(view, this)
        }

        translationX = offsetRect.left.toFloat()
        translationY = offsetRect.top.toFloat()

        val xAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_X, offsetRect.left.toFloat(), 0f)

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offsetRect.top.toFloat(), 0f)

        val widthAnimator = ObjectAnimator.ofInt(this, "width", view.width, appState.screenWidth)
        val heightAnimator =
            ObjectAnimator.ofInt(this, "height", view.height, appState.screenHeight)

        AnimatorSet().run {
            duration = 200
            interpolator = PathInterpolator(0.33f, 1f, 0.68f, 1f)
            playTogether(widthAnimator, heightAnimator, xAnimator, yAnimator)
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

        val widthAnimator = ObjectAnimator.ofInt(this, "width", originalView.width)
        val heightAnimator = ObjectAnimator.ofInt(this, "height", originalView.height)

        AnimatorSet().run {
            duration = 200
            interpolator = PathInterpolator(0.33f, 1f, 0.68f, 1f)

            playTogether(widthAnimator, heightAnimator, xAnimator, yAnimator)
            addListener({
                isVisible = false
                isClosing = false
                pauseBlur()
            })

            start()
        }
    }
}