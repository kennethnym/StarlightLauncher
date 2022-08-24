package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.animation.PathInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.addListener
import androidx.core.view.*
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.view.Plate
import kenneth.app.starlightlauncher.api.util.activity
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
internal class Overlay(context: Context, attrs: AttributeSet) :
    Plate(context, attrs),
    ViewTreeObserver.OnGlobalFocusChangeListener,
    OnApplyWindowInsetsListener {
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

    private var focusedView: View? = null
    private var originalTranslationY: Float? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isVisible && !isClosing) {
                close()
            }
        }
    }

    init {
        ViewCompat.setWindowInsetsAnimationCallback(this, InsetsAnimation())
        ViewCompat.setOnApplyWindowInsetsListener(this, this)
        viewTreeObserver.addOnGlobalFocusChangeListener(this)
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

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // translate the overlay for the inset
        // this translation will be animated by InsetsAnimation

        val focusedView = this.focusedView ?: return insets

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
            translationY -= Integer.max(0, y + 100 - imeY)
        } else {
            originalTranslationY?.let { translationY = it }
        }

        return insets
    }

    override fun onDestroy(owner: LifecycleOwner) {
        viewTreeObserver.removeOnGlobalFocusChangeListener(this)
        super.onDestroy(owner)
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        focusedView = newFocus
    }

    private fun displayContent(content: View) {
        if (childCount > 1) {
            removeViewAt(1)
        }
        addView(content)
    }

    private inner class InsetsAnimation : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var startTranslationY: Float? = null
        private var endTranslationY: Float? = null

        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
            Log.d("starlight", "startTranslationY $translationY")
            startTranslationY = translationY
            super.onPrepare(animation)
        }

        override fun onStart(
            animation: WindowInsetsAnimationCompat,
            bounds: WindowInsetsAnimationCompat.BoundsCompat
        ): WindowInsetsAnimationCompat.BoundsCompat {
            endTranslationY = translationY
            originalTranslationY?.let { translationY = it }
            return super.onStart(animation, bounds)
        }

        override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>
        ): WindowInsetsCompat {
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