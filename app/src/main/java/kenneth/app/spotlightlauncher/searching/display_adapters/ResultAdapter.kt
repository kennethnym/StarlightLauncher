package kenneth.app.spotlightlauncher.searching.display_adapters

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.searching.SmartSearcher
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

    @Provides
    fun provideSuggestedResultAdapter(@ActivityContext context: Context) =
        SuggestedResultAdapter(context as MainActivity)

    @Provides
    fun provideWebResultAdapter(@ActivityContext context: Context) =
        WebResultAdapter(context as MainActivity)
}

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class ResultAdapter @Inject constructor(
    private val webResultAdapter: WebResultAdapter,
    private val appsGridAdapter: AppsGridDataAdapter,
    private val fileListAdapter: FileListDataAdapter,
    private val suggestedResultAdapter: SuggestedResultAdapter
) {
    fun displayResult(result: Searcher.Result, type: SearchType) {
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
        }
    }

    fun displayWebResult(result: SmartSearcher.WebResult) {
        webResultAdapter.displayResult(result)
    }
}