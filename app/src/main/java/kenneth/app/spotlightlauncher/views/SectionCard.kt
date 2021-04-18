package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.dp

private const val DEFAULT_TITLE = "Section card title"

/**
 * A card that is displayed in the search result page.
 */
open class SectionCard<TData>(context: Context, attrs: AttributeSet) : BlurView(context, attrs) {
    /**
     * The title of this card.
     */
    var title = DEFAULT_TITLE
        get() = cardTitle.text.toString()
        set(newTitle) {
            field = newTitle
            cardTitle.text = newTitle
        }

    private lateinit var cardTitle: AdaptiveColorTextView

    private lateinit var mainLayout: LinearLayout

    init {
        background = context.getDrawable(R.drawable.card_background)
        blurAmount = context.obtainStyledAttributes(intArrayOf(R.attr.blurAmount)).run {
            try {
                getResourceId(0, 10)
            } finally {
                recycle()
            }
        }
    }

    @CallSuper
    open fun display(data: TData) {
        isVisible = true
        startBlur()
    }

    /**
     * Hides this card from the search result page
     */
    @CallSuper
    open fun hide() {
        pauseBlur()
        isVisible = false
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (!::mainLayout.isInitialized) {
            createLayout()
        }

        mainLayout.addView(child, params)
    }

    private fun createLayout() {
        cardTitle = AdaptiveColorTextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(16.dp) }

            text = DEFAULT_TITLE
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        }

        mainLayout = LinearLayout(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            fitsSystemWindows = false
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
            updatePadding(bottom = 16.dp)

            addView(cardTitle, 0)
        }

        addView(mainLayout)
    }
}