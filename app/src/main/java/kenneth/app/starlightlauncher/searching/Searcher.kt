package kenneth.app.starlightlauncher.searching

import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 500

typealias ResultCallback = (SearchResult) -> Unit

@Singleton
internal class Searcher @Inject constructor(
    private val extensionManager: ExtensionManager,
    @Named(MAIN_DISPATCHER) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher
) {
    private val resultCallbacks = mutableListOf<ResultCallback>()

    private val searchCoroutineScopes = mutableListOf<CoroutineScope>()

    /**
     * Counts the number of search categories that have finished loading.
     * For example, if [Searcher] has finished searching through apps,
     * this counter is incremented by 1.
     */
    private var numberOfLoadedModules = 0

    private var searchTimer: TimerTask? = null

    /**
     * Adds a listener that is called when search result is available.
     */
    fun addSearchResultListener(callback: ResultCallback) {
        resultCallbacks.add(callback)
    }

    /**
     * Requests to search for all types after a set delay (currently set to 1 second)
     */
    fun requestSearch(keyword: String) {
        searchTimer?.cancel()
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
        searchTimer?.cancel()
        numberOfLoadedModules = 0
    }

    private fun performSearch(keyword: String) {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        extensionManager.installedSearchModules.forEach { module ->
            CoroutineScope(mainDispatcher)
                .also { searchCoroutineScopes.add(it) }
                .launch {
                    val result = withContext(ioDispatcher) {
                        module.search(keyword, searchRegex)
                    }
                    numberOfLoadedModules++
                    resultCallbacks.forEach { cb -> cb(result) }
                }
        }
    }
}
