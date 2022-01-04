package kenneth.app.spotlightlauncher.api.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kenneth.app.spotlightlauncher.api.R

open class Plate(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        context.obtainStyledAttributes(intArrayOf(R.attr.plateColor)).run {
            try {
                setBackgroundColor(getColor(0, context.getColor(android.R.color.transparent)))
            } finally {
                recycle()
            }
        }
    }
}