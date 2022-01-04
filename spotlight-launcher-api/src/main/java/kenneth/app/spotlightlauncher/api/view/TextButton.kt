package kenneth.app.spotlightlauncher.api.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.TextViewCompat
import kenneth.app.spotlightlauncher.api.R

class TextButton(
    context: Context,
    attrs: AttributeSet?,
) :
    AppCompatButton(context, attrs, R.style.Style_SpotlightLauncher_TextButton) {
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