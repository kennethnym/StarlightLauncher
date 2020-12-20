package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import kenneth.app.spotlightlauncher.R

private const val OPACITY_ENABLED = 1f
private const val OPACITY_CLICKED = 0.5f
private const val OPACITY_DISABLED = 0.2f

class IconButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {
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