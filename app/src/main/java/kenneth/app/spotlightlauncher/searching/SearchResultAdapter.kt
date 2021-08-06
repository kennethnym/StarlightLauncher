package kenneth.app.spotlightlauncher.searching

import android.content.pm.ResolveInfo
import androidx.core.view.isVisible
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class SearchResultAdapter @Inject constructor() {
    fun displayResult(result: SearchResult) {
        BindingRegister.activityMainBinding.widgetsPanel.showSearchResults()

        with(BindingRegister.searchResultViewBinding) {
            when (result) {
                is SearchResult.Files -> {
                    filesSectionCard.display(result.files)
                }
                is SearchResult.Apps -> {
                    appsSectionCard.display(result.apps)
                }
                is SearchResult.Suggested -> {
                    if (result !is SearchResult.Suggested.App || !suggesedResultCard.isVisible) {
                        suggesedResultCard.display(result)
                    }
                }
                else -> {}
            }
        }
    }

    fun hideResult() {
        with(BindingRegister.searchResultViewBinding) {
            appsSectionCard.hide()
            filesSectionCard.hide()
            webResultCard.hide()
            suggesedResultCard.hide()
        }

        BindingRegister.activityMainBinding.widgetsPanel.hideSearchResults()
    }

    fun displayWebResult(result: SmartSearcher.WebResult) {
        BindingRegister.searchResultViewBinding.webResultCard.display(result)
    }

    /**
     * Perform cleanup work on adapters
     */
    fun cleanup() {

    }
}