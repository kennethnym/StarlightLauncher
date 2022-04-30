package kenneth.app.starlightlauncher.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import kenneth.app.starlightlauncher.R

class WidgetFrame(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        togglePadding()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        togglePadding()
    }

    /**
     * Toggles padding of the frame based on the visibility of the widget.
     */
    private fun togglePadding() {
        if (children.first().visibility == View.GONE) {
            setPadding(0)
        } else {
            val paddingHorizontal = context.resources.getDimensionPixelSize(R.dimen.widget_list_spacing)
            val paddingVertical = context.resources.getDimensionPixelSize(R.dimen.widget_space_between)

            updatePadding(
                left = paddingHorizontal,
                right = paddingHorizontal,
                top = paddingVertical,
                bottom = paddingVertical
            )
        }
    }
}