package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import androidx.annotation.IdRes
import androidx.core.animation.addListener
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.LauncherState
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.view.Plate
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
    @IdRes
    val contentContainerId = R.id.overlay_container_view

    @Inject
    lateinit var launcherState: LauncherState

    @Inject
    lateinit var blurHandler: BlurHandler

    /**
     * Whether the closing animation of [Overlay] is being played.
     */
    private var isClosing = false

    private val root: View

    private var focusedView: View? = null
    private var originalTranslationY: Float? = null

    init {
        root = LayoutInflater.from(context).inflate(R.layout.overlay, this)
        backgroundAlpha = 220

        ViewCompat.setWindowInsetsAnimationCallback(this, InsetsAnimation())
        ViewCompat.setOnApplyWindowInsetsListener(this, this)
        viewTreeObserver.addOnGlobalFocusChangeListener(this)
    }

    fun show() {
        isVisible = true
        translationY = launcherState.screenHeight.toFloat()

        blurWith(blurHandler)

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, launcherState.screenHeight.toFloat(), 0f)

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR
            play(yAnimator)
            start()
        }
    }

    fun close() {
        isClosing = true

        val yAnimator =
            ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, launcherState.screenHeight.toFloat())

        AnimatorSet().run {
            duration = SHOW_OVERLAY_ANIMATION_DURATION
            interpolator = SHOW_OVERLAY_ANIMATION_PATH_INTERPOLATOR

            play(yAnimator)
            addListener({
                isVisible = false
                isClosing = false
            })

            start()
        }
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
        val imeY = launcherState.screenHeight - imeHeight

        if (imeHeight > 0) {
            originalTranslationY = translationY
            translationY -= Integer.max(0, y + 100 - imeY)
        } else {
            originalTranslationY?.let { translationY = it }
        }

        return insets
    }

    override fun onGlobalFocusChanged(oldFocus: View?, newFocus: View?) {
        focusedView = newFocus
    }

    private inner class InsetsAnimation : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
        private var startTranslationY: Float? = null
        private var endTranslationY: Float? = null

        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
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