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
import kenneth.app.spotlightlauncher.views.BlurView

class SuggestedResultAdapter(activity: MainActivity) :
    SectionResultAdapter<SmartSearcher.SuggestedResult>(activity) {
    /**
     * The entire card view that displays suggested result
     */
    private lateinit var cardContainer: LinearLayout

    private lateinit var cardBlurView: BlurView

    /**
     * The view that contains the suggested result content
     */
    private lateinit var suggestedContentContainer: LinearLayout

    private val wifiController = WifiController(activity)
    private val bluetoothController = BluetoothController(activity)

    override fun displayResult(result: SmartSearcher.SuggestedResult) {
        with(activity) {
            cardContainer = findViewById(R.id.suggested_section_card)

            cardBlurView = findViewById<BlurView>(R.id.suggested_section_card_blur_background)
                .also { it.startBlur() }

            suggestedContentContainer = findViewById<LinearLayout>(R.id.suggested_content)
                .also { it.removeAllViews() }
        }

        if (result.type != SuggestedResultType.NONE) {
            cardContainer.visibility = View.VISIBLE

            when (result.type) {
                SuggestedResultType.MATH -> displayMathResult(result)
                SuggestedResultType.WIFI -> wifiController.displayWifiControl(
                    suggestedContentContainer
                )
                SuggestedResultType.BLUETOOTH -> bluetoothController.displayBluetoothControl(
                    suggestedContentContainer
                )
                else -> {
                }
            }
        } else {
            hideSuggestedResult()
        }
    }

    fun hideSuggestedResult() {
        if (::cardContainer.isInitialized && ::cardBlurView.isInitialized) {
            cardBlurView.pauseBlur()
            cardContainer.visibility = View.GONE
        }
    }

    private fun displayMathResult(result: SmartSearcher.SuggestedResult) {
        val equationText = activity.findViewById<TextView>(R.id.equation_text)
        val inflated = equationText != null
        val resultStr = "= ${result.result}"

        if (!inflated) {
            LayoutInflater.from(activity)
                .inflate(R.layout.math_result_layout, suggestedContentContainer)
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
}

