package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kenneth.app.spotlightlauncher.searching.modules.AppSearchModule
import kenneth.app.spotlightlauncher.searching.modules.FileSearchModule
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 500

typealias ResultCallback = (SearchResult) -> Unit

@ActivityScoped
class Searcher @Inject constructor(
    @ActivityContext private val context: Context,
    private val smartSearcher: SmartSearcher,
    appManager: AppManager,
    filePreferenceManager: FilePreferenceManager,
) {
    val hasFinishedSearching: Boolean
        get() = numberOfLoadedModules == searchModules.size

    private val resultCallbacks = mutableListOf<ResultCallback>()

    private val searchModules = mutableListOf(
        AppSearchModule(appManager),
        FileSearchModule(context, filePreferenceManager)
    )

    private val searchCoroutineScopes = mutableListOf<CoroutineScope>()

    /**
     * Counts the number of search categories that have finished loading.
     * For example, if [Searcher] has finished searching through apps,
     * this counter is incremented by 1.
     */
    private var numberOfLoadedModules = 0

    private lateinit var searchTimer: TimerTask

    init {
        context.registerReceiver(
            appManager.PackageObserver(),
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        )
    }

    /**
     * Adds a listener that is called when search result is available.
     */
    fun addSearchResultListener(callback: ResultCallback) {
        resultCallbacks.add(callback)
    }

    fun setWebResultListener(callback: WebResultCallback) {
        smartSearcher.setWebResultListener(callback)
    }

    /**
     * Requests to search for all types after a set delay (currently set to 1 second)
     */
    fun requestSearch(keyword: String) {
        if (::searchTimer.isInitialized) cancelPendingSearch()

        searchTimer = Timer().schedule(SEARCH_DELAY) {
            performSearch(keyword)
        }
        numberOfLoadedModules = 0
    }

    /**
     * Cancels any pending search requests
     */
    fun cancelPendingSearch() {
        searchCoroutineScopes.forEach { it.cancel() }
        if (::searchTimer.isInitialized) {
            searchTimer.cancel()
        }
        numberOfLoadedModules = 0
    }

    private fun performSearch(keyword: String) {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        searchModules.forEach {
            CoroutineScope(Dispatchers.IO)
                .also { searchCoroutineScopes.add(it) }
                .run {
                    launch {
                        val result = withContext(coroutineContext) {
                            it.search(keyword, searchRegex)
                        }
                        numberOfLoadedModules++
                        resultCallbacks.forEach { cb -> cb(result) }
                    }
                }
        }
    }
}
