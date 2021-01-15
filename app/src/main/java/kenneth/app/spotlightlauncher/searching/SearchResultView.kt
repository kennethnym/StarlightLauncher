package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.setPadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.SettingsActivity
import kenneth.app.spotlightlauncher.searching.display_adapters.ResultAdapter
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.utils.dp
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var searchResultAdapter: ResultAdapter

    private val settingsIntent = Intent(context, SettingsActivity::class.java)

    init {
        fitsSystemWindows = false
        orientation = VERTICAL
        gravity = Gravity.START

        setPadding(16.dp)

        inflate(context, R.layout.search_result_layout, this)

        with(searcher) {
            setSearchResultListener { result, type ->
                activity?.runOnUiThread {
                    searchResultAdapter.displayResult(result, type)
                }
            }

            setWebResultListener { result ->
                activity?.runOnUiThread {
                    searchResultAdapter.displayWebResult(result)
                }
            }
        }

        findViewById<Button>(R.id.open_settings_button)
            .setOnClickListener { openSettings() }
    }

    private fun openSettings() {
        context.startActivity(settingsIntent)
    }
}