package kenneth.app.spotlightlauncher.searching.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.core.view.isVisible
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.SuggestedResultCardBinding
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.searching.SuggestedResultType
import kenneth.app.spotlightlauncher.views.BlurView
import kenneth.app.spotlightlauncher.views.SectionCard

/**
 * Displayed at the top of the search page to show the user suggested result from the launcher.
 */
class SuggestedResultCard(context: Context, attrs: AttributeSet) :
    SectionCard<SmartSearcher.SuggestedResult>(context, attrs) {
    private val binding = SuggestedResultCardBinding.inflate(LayoutInflater.from(context), this)

    init {
        title = context.getString(R.string.suggested_section_title)
        blurAmount = TypedValue().run {
            context.theme.resolveAttribute(R.attr.blurAmount, this, true)
            data
        }
    }

    /**
     * Displays the given suggested result in this card.
     */
    override fun display(result: SmartSearcher.SuggestedResult) {
        binding.suggestedContent.removeAllViews()

        if (result.type != SuggestedResultType.NONE) {
            super.display(result)

            when (result.type) {
                SuggestedResultType.WIFI -> {
                    binding.suggestedContent.addView(WifiControl(context))
                }
                SuggestedResultType.BLUETOOTH -> {
                    binding.suggestedContent.addView(BluetoothControl(context))
                }
                SuggestedResultType.URL -> {
                    binding.suggestedContent.addView(
                        OpenUrlControl(context).apply {
                            url = result.query
                        }
                    )
                }
                SuggestedResultType.MATH -> {
                    binding.suggestedContent.addView(
                        MathResultView(context).also {
                            it.showResult(result)
                        }
                    )
                }
                SuggestedResultType.APP -> {
                    binding.suggestedContent.addView(
                        SuggestedAppView(context).apply {
                            setSuggestedApp(result.result as ResolveInfo)
                        }
                    )
                }
                else -> hide()
            }
        }
    }
}