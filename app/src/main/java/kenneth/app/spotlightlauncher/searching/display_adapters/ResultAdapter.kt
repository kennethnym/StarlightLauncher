package kenneth.app.spotlightlauncher.searching.display_adapters

import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class ResultAdapter(activity: MainActivity) {
    private val appsGridAdapter = AppsGridAdapter.initializeWith(activity)
    private val fileListAdapter = FileListAdapter.initializeWith(activity)
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
}