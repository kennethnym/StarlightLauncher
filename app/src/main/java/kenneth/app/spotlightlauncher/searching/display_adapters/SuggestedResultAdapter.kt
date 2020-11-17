package kenneth.app.spotlightlauncher.searching.display_adapters

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.searching.SuggestedResultType

class SuggestedResultAdapter(private val activity: MainActivity) {
    fun displayResult(result: SmartSearcher.SuggestedResult) {
        val card = activity.findViewById<MaterialCardView>(R.id.suggested_section_card)

        if (result.type != SuggestedResultType.NONE) {
            card.apply {
                visibility = View.VISIBLE
            }

            val contentParent =
                activity.findViewById<LinearLayout>(R.id.suggested_section_card_layout)

            when (result.type) {
                SuggestedResultType.MATH -> {
                    val equationText = activity.findViewById<TextView>(R.id.equation_text)
                    val inflated = equationText != null
                    val resultStr = "= ${result.result}"

                    if (!inflated) {
                        LayoutInflater.from(activity)
                            .inflate(R.layout.math_result_layout, contentParent)
                            .also {
                                it.findViewById<TextView>(R.id.equation_text).text = result.query
                                it.findViewById<TextView>(R.id.equation_result_text).text =
                                    resultStr
                            }
                    } else {
                        equationText.text = result.query
                        activity.findViewById<TextView>(R.id.equation_result_text).text =
                            resultStr
                    }
                }
                else -> {
                }
            }
        } else {
            card?.visibility = View.GONE
        }
    }
}
