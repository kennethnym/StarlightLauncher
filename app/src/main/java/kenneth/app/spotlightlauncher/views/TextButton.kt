package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.NOT_HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

private const val USE_ADAPTIVE_COLOR = Integer.MAX_VALUE

@AndroidEntryPoint
class TextButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatButton(context, attrs, R.style.TextButton),
    LifecycleObserver {
    @Inject
    lateinit var appState: AppState

    private val shouldUseAdaptiveColor: Boolean

    init {
        activity?.lifecycle?.addObserver(this)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TextButton,
            0, 0
        ).run {
            try {
                val overriddenTextColor =
                    getColor(R.styleable.TextButton_color, USE_ADAPTIVE_COLOR)
                shouldUseAdaptiveColor = overriddenTextColor == USE_ADAPTIVE_COLOR

                if (!shouldUseAdaptiveColor) {
                    compoundDrawablesRelative.forEach { it?.setTint(overriddenTextColor) }
                    setTextColor(overriddenTextColor)
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shouldUseAdaptiveColor) {
            setTextColor(appState.adaptiveTextColor)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return HANDLED
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        return when (event?.action) {
            MotionEvent.ACTION_BUTTON_PRESS -> {
                performClick()
                HANDLED
            }
            MotionEvent.ACTION_DOWN -> {
                showClickedEffect()
                HANDLED
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hideClickedEffect()
                HANDLED
            }
            else -> NOT_HANDLED
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        if (shouldUseAdaptiveColor) {
            setTextColor(appState.adaptiveTextColor)
        }
    }

    private fun showClickedEffect() {
        alpha = 0.5f
    }

    private fun hideClickedEffect() {
        alpha = 1f
    }
}