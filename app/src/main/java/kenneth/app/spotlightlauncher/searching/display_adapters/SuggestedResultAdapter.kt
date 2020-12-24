package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.searching.SuggestedResultType
import kenneth.app.spotlightlauncher.searching.display_adapters.bluetooth.BluetoothController
import kenneth.app.spotlightlauncher.views.BlurView
import javax.inject.Inject

@Module
@InstallIn(ActivityComponent::class)
object SuggestedResultAdapterModule {
    @Provides
    fun provideWifiController(
        mainActivity: MainActivity?,
        wifiManager: WifiManager
    ): WifiController? =
        if (mainActivity == null) {
            null
        } else {
            WifiController(mainActivity, wifiManager)
        }
}

class SuggestedResultAdapter @Inject constructor(
    private val activity: Activity,
    private val bluetoothController: BluetoothController,
    private val wifiController: WifiController?,
) :
    SectionResultAdapter<SmartSearcher.SuggestedResult>() {
    /**
     * The entire card view that displays suggested result
     */
    private lateinit var cardContainer: LinearLayout

    private lateinit var cardBlurView: BlurView

    /**
     * The view that contains the suggested result content
     */
    private lateinit var suggestedContentContainer: LinearLayout

    override fun displayResult(result: SmartSearcher.SuggestedResult) {
        with(activity) {
            cardContainer = findViewById(R.id.suggested_section_card)
            cardBlurView = findViewById(R.id.suggested_section_card_blur_background)
            suggestedContentContainer = findViewById<LinearLayout>(R.id.suggested_content)
                .also { it.removeAllViews() }
        }

        if (result.type != SuggestedResultType.NONE) {
            cardContainer.isVisible = true
            cardBlurView.startBlur()

            when (result.type) {
                SuggestedResultType.MATH -> displayMathResult(result)
                SuggestedResultType.WIFI -> {
                    if (wifiController != null) {
                        wifiController.displayWifiControl(
                            suggestedContentContainer
                        )
                    } else {
                        cardContainer.isVisible = false
                        cardBlurView.pauseBlur()
                    }
                }
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

