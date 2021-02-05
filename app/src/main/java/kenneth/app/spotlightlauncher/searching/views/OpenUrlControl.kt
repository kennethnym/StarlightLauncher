package kenneth.app.spotlightlauncher.searching.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.setPadding
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.OpenUrlControlBinding
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.searching.SuggestedResultType

/**
 * Renders a view that lets user open the copied link in the browser when
 * [SuggestedResultType.URL] is suggested by [SmartSearcher].
 */
class OpenUrlControl(context: Context) : LinearLayout(context) {
    private val binding: OpenUrlControlBinding

    /**
     * The URL that this control should open. Must be set on render.
     */
    var url: String = ""
        set(url) {
            field = url
            binding.urlOpenerUrlLabel.text = url
        }

    init {
        val bgDrawableId = TypedValue().run {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
            resourceId
        }
        background = context.getDrawable(bgDrawableId)
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        setPadding(resources.getDimensionPixelSize(R.dimen.section_card_padding))

        binding = OpenUrlControlBinding.inflate(LayoutInflater.from(context), this)

        setOnClickListener { open(url) }
    }

    /**
     * Opens the given url in the default browser.
     */
    private fun open(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fixURL(url)))

        if (browserIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(browserIntent)
        }
    }

    /**
     * Fixes missing https issues in the given url
     */
    private fun fixURL(url: String): String =
        if (url.startsWith("www."))
            "https://$url"
        else url
}