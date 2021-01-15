package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.searching.SearchResultView
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.searching.display_adapters.suggested.SuggestedResultAdapter
import javax.inject.Inject

/**
 * A module that provides all adapters needed to display search results.
 */
@Module
@InstallIn(ActivityComponent::class)
object AdaptersModule {
    @Provides
    fun provideAppsGridAdapter(@ActivityContext context: Context) =
        AppsGridDataAdapter.getInstance(context as MainActivity)

    @Provides
    fun provideFileListAdapter(@ActivityContext context: Context) =
        FileListDataAdapter.getInstance(context as MainActivity)
}

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
@ActivityScoped
class ResultAdapter @Inject constructor(
    private val activity: Activity,
    private val webResultAdapter: WebResultAdapter,
    private val appsGridAdapter: AppsGridDataAdapter,
    private val fileListAdapter: FileListDataAdapter,
    private val suggestedResultAdapter: SuggestedResultAdapter
) {
    private lateinit var widgetListContainer: LinearLayout
    private lateinit var searchResultContainer: SearchResultView

    fun displayResult(result: Searcher.Result, type: SearchType) {
        with(activity) {
            widgetListContainer = findViewById<LinearLayout>(R.id.widget_list_container).apply {
                isVisible = false
            }
            searchResultContainer =
                findViewById<SearchResultView>(R.id.search_result_container).apply {
                    isVisible = true
                }
        }

        when (type) {
            SearchType.ALL -> {
                appsGridAdapter.displayData(result.apps)
                fileListAdapter.displayData(result.files)
                suggestedResultAdapter.displayResult(result.suggested)
            }
            SearchType.FILES -> {
                fileListAdapter.displayData(result.files)
            }
            SearchType.APPS -> {
                appsGridAdapter.displayData(result.apps)
            }
            SearchType.SUGGESTED -> {
                suggestedResultAdapter.displayResult(result.suggested)
            }
        }
    }

    fun hideResult() {
        appsGridAdapter.hideAppsGrid()
        fileListAdapter.hideFileList()
        webResultAdapter.hideWebResult()
        suggestedResultAdapter.hideSuggestedResult()

        if (::searchResultContainer.isInitialized) {
            searchResultContainer.isVisible = false
        }
    }

    fun displayWebResult(result: SmartSearcher.WebResult) {
        webResultAdapter.displayResult(result)
    }

    /**
     * Perform cleanup work on adapters
     */
    fun cleanup() {

    }
}