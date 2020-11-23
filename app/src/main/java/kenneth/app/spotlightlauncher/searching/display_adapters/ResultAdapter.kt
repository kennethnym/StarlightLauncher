package kenneth.app.spotlightlauncher.searching.display_adapters

import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.searching.SmartSearcher

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class ResultAdapter(private val activity: MainActivity) {
    private lateinit var webResultAdapter: WebResultAdapter

    private val appsGridAdapter = AppsGridAdapter.getInstance(activity)
    private val fileListAdapter = FileListAdapter.getInstance(activity)
    private val suggestedResultAdapter = SuggestedResultAdapter(activity)

    fun displayResult(result: Searcher.Result, type: SearchType) {
        when (type) {
            SearchType.ALL -> {
                appsGridAdapter.displayResult(result.apps)
                fileListAdapter.displayResult(result.files)
                suggestedResultAdapter.displayResult(result.suggested)
            }
            SearchType.FILES -> {
                fileListAdapter.displayResult(result.files)
            }
            SearchType.APPS -> {
                appsGridAdapter.displayResult(result.apps)
            }
        }
    }

    fun displayWebResult(result: SmartSearcher.WebResult) {
        if (!::webResultAdapter.isInitialized) {
            webResultAdapter = WebResultAdapter(activity)
        }

        webResultAdapter.displayResult(result)
    }
}