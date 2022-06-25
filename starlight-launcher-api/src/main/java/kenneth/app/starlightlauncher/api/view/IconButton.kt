package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.TooltipCompat
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.util.dp

/**
 * Whether [IconButton] should show the icon with an adaptive color by default.
 */
private const val DEFAULT_USE_ADAPTIVE_COLOR = true
private val DEFAULT_ICON_SIZE = 16.dp.toFloat()
private const val OPACITY_ENABLED = 1f
private const val OPACITY_CLICKED = 0.5f
private const val OPACITY_DISABLED = 0.2f

class IconButton(context: Context, attrs: AttributeSet?) :
    AppCompatImageView(context, attrs, R.style.Style_StarlightLauncher_IconButton) {
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
    var icon: Drawable? = drawable
        get() = drawable
        set(drawable) {
            field = drawable
            setImageDrawable(drawable)
        }

    init {
        if (contentDescription?.isNotEmpty() == true) {
            TooltipCompat.setTooltipText(this, contentDescription)
        }
    }

}