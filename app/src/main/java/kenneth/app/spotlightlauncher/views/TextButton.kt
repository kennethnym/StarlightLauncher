package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
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

/**
 * Whether [TextButton] should show the content with an adaptive color by default.
 */
private const val DEFAULT_USE_CUSTOM_COLOR = false

@AndroidEntryPoint
class TextButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatButton(context, attrs, R.style.TextButton),
    LifecycleObserver {
    @Inject
    lateinit var appState: AppState

    private val shouldUseAdaptiveColor: Boolean

    init {
        gravity = Gravity.CENTER

        activity?.lifecycle?.addObserver(this)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TextButton,
            0, 0
        ).run {
            try {
                val textColor =
                    getColor(R.styleable.TextButton_color, appState.adaptiveTheme.adaptiveTextColor)

                shouldUseAdaptiveColor =
                    !getBoolean(R.styleable.TextButton_useCustomColor, DEFAULT_USE_CUSTOM_COLOR)

                compoundDrawablesRelative.forEach { it?.setTint(textColor) }
                setTextColor(textColor)
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shouldUseAdaptiveColor) {
            setTextColor(appState.adaptiveTheme.adaptiveTextColor)
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
            setTextColor(appState.adaptiveTheme.adaptiveTextColor)
        }
    }

    private fun showClickedEffect() {
        alpha = 0.5f
    }

    private fun hideClickedEffect() {
        alpha = 1f
    }
}