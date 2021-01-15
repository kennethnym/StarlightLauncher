package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.animations.CardAnimation

/**
 * Contains a list of widgets on the home screen.
 */
class WidgetList(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val animations: List<CardAnimation>

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        inflate(context, R.layout.widget_list, this)

        animations = generateAnimations()
    }

    /**
     * Shows all the widgets in this list.
     */
    fun showWidgets() {
        animations.forEach { it.showCard() }
    }

    /**
     * Hides all the widgets in this list. Note that this does not remove children in the layout.
     */
    fun hideWidgets() {
        animations.forEach { it.hideCard() }
    }

    /**
     * Generates card animations for every widget.
     */
    private fun generateAnimations(): List<CardAnimation> =
        children.foldIndexed(mutableListOf()) { i, anims, child ->
            if (child.isVisible) {
                anims.add(CardAnimation(child, i * 500L))
            }
            anims
        }
}
