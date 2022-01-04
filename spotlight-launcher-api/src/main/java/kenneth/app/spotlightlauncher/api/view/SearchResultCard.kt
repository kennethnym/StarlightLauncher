package kenneth.app.spotlightlauncher.api.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kenneth.app.spotlightlauncher.api.R
import kenneth.app.spotlightlauncher.api.databinding.SearchResultCardLayoutBinding

const val DEFAULT_CARD_TITLE = "Search result"

/**
 * A card that displays search results of a particular [SearchModule],
 * with a blurred background and a title.
 * Children of [SearchResultCard] are placed inside of a [LinearLayout].
 *
 * All default search modules like apps and files uses this to displays
 * their search results.
 */
open class SearchResultCard(context: Context, attrs: AttributeSet?) : Plate(context, attrs) {
    private val binding =
        SearchResultCardLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * The title of [SearchResultCard]
     */
    var title = DEFAULT_CARD_TITLE
        /**
         * Changes the title of [SearchResultCard]
         */
        set(newTitle) {
            field = newTitle
            binding.cardTitle.text = newTitle
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SearchResultCard).run {
            try {
                title = getString(R.styleable.SearchResultCard_cardTitle) ?: DEFAULT_CARD_TITLE
            } finally {
                recycle()
            }
        }
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        binding.content.addView(child, params)
    }
}
