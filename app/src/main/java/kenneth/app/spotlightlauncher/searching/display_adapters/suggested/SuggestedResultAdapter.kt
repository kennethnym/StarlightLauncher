package kenneth.app.spotlightlauncher.searching.display_adapters.suggested

import android.app.Activity
import android.net.wifi.WifiManager
import android.view.LayoutInflater
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
import kenneth.app.spotlightlauncher.searching.display_adapters.SectionResultAdapter
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

    @Provides
    fun provideURLOpener(mainActivity: MainActivity?) =
        if (mainActivity == null)
            null
        else
            URLOpener(mainActivity)
}

class SuggestedResultAdapter @Inject constructor(
    private val activity: Activity,
    private val bluetoothController: BluetoothController,
    private val wifiController: WifiController?,
    private val urlOpener: URLOpener?,
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
        findViews()

        suggestedContentContainer.removeAllViews()

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
                SuggestedResultType.URL -> urlOpener?.displayControl(
                    suggestedContentContainer,
                    url = result.query
                )
                else -> {
                }
            }
        } else {
            hideSuggestedResult()
        }
    }

    /**
     * Hides the views associated with this adapter.
     */
    fun hideSuggestedResult() {
        if (::cardContainer.isInitialized && ::cardBlurView.isInitialized) {
            cardBlurView.apply {
                pauseBlur()
                isVisible = false
            }
            cardContainer.isVisible = false
        }
    }

    private fun findViews() {
        with(activity) {
            if (!::cardContainer.isInitialized) {
                cardContainer = findViewById(R.id.suggested_section_card)
            }

            if (!::cardBlurView.isInitialized) {
                cardBlurView = findViewById(R.id.suggested_section_card_blur_background)
            }

            if (!::suggestedContentContainer.isInitialized) {
                suggestedContentContainer = findViewById(R.id.suggested_content)
            }
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

