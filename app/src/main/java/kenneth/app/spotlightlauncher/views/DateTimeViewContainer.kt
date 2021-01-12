package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R

/**
 * A simple LinearLayout wrapper that
 * contains DateTimeView and media control widget if there is media currently playing.
 *
 * Contains a layoutWeight getter/setter to enable ObjectAnimator animation.
 */
class DateTimeViewContainer(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var layoutWeight: Float
        get() = (layoutParams as LayoutParams).weight
        set(newWeight) {
            val newLayoutParams = (layoutParams as LayoutParams).apply {
                weight = newWeight
            }
            layoutParams = newLayoutParams
        }
}