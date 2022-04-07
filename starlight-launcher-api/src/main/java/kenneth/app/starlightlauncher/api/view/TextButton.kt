package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.TextViewCompat
import kenneth.app.starlightlauncher.api.R

class TextButton(
    context: Context,
    attrs: AttributeSet?,
) :
    AppCompatButton(context, attrs, R.style.Style_StarlightLauncher_TextButton) {
    var icon: Drawable?
        get() = compoundDrawables[0]
        set(icon) = setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)

    init {
        gravity = Gravity.CENTER_VERTICAL
        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textColor)).run {
            try {
                TextViewCompat.setCompoundDrawableTintList(this@TextButton, getColorStateList(0))
            } finally {
                recycle()
            }
        }
    }
}