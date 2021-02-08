package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

private const val OPACITY_ENABLED = 1f
private const val OPACITY_CLICKED = 0.5f
private const val OPACITY_DISABLED = 0.2f

@AndroidEntryPoint
class IconButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs), LifecycleObserver {
    /**
     * Whether this button should be disabled, thus not clickable.
     */
    var disabled: Boolean = false
        set(disabled) {
            field = disabled
            alpha = if (disabled) OPACITY_DISABLED else OPACITY_ENABLED
        }

    /**
     * The Drawable icon of this IconButton
     */
    var icon: Drawable? = null
        set(drawable) {
            field = drawable
            setImageDrawable(drawable)
        }

    @Inject
    lateinit var appState: AppState

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.IconButton,
            0, 0
        ).apply {
            try {
                icon = getDrawable(R.styleable.IconButton_icon)
            } finally {
                recycle()
            }
        }

        activity?.lifecycle?.addObserver(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setColorFilter(appState.adaptiveTextColor)
    }

    override fun performClick(): Boolean {
        if (disabled) return false
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        return when (event?.action) {
            MotionEvent.ACTION_BUTTON_PRESS -> {
                performClick()
                true
            }
            MotionEvent.ACTION_DOWN -> {
                showClickedEffect()
                true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hideClickedEffect()
                true
            }
            else -> false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        setColorFilter(appState.adaptiveTextColor)
    }

    private fun showClickedEffect() {
        if (!disabled) {
            alpha = OPACITY_CLICKED
        }
    }

    private fun hideClickedEffect() {
        if (!disabled) {
            alpha = OPACITY_ENABLED
        }
    }
}