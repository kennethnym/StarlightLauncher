package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.TextViewCompat
import kenneth.app.starlightlauncher.api.R

class TextButton(
    context: Context,
    attrs: AttributeSet?,
) :
    AppCompatButton(context, attrs, R.style.Style_SpotlightLauncher_TextButton) {
    var icon: Drawable?
        get() = compoundDrawables[0]
        set(icon) = setCompoundDrawablesRelative(icon, null, null, null)

    init {
        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textColor)).run {
            try {
                TextViewCompat.setCompoundDrawableTintList(this@TextButton, getColorStateList(0))
            } finally {
                recycle()
            }
        }
    }
}