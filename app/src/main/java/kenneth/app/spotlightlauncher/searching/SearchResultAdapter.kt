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
    fun displayResult(result: Searcher.Result, category: SearchCategory) {
        BindingRegister.activityMainBinding.widgetsPanel.showSearchResults()

        with(BindingRegister.searchResultViewBinding) {
            when (category) {
                SearchCategory.ALL -> {
                    appsSectionCard.display(result.apps)
                    filesSectionCard.display(result.files)
                    suggesedResultCard.display(result.suggested)
                }
                SearchCategory.FILES -> {
                    filesSectionCard.display(result.files)
                }
                SearchCategory.APPS -> {
                    appsSectionCard.display(result.apps)
                }
                SearchCategory.SUGGESTED -> {
                    if (result.suggested.result !is ResolveInfo || !suggesedResultCard.isVisible) {
                        suggesedResultCard.display(result.suggested)
                    }
                }
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