package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 500

typealias ResultCallback = (SearchResult) -> Unit

/**
 * Different categories that Searcher will search for
 */
enum class SearchCategory {
    APPS, FILES, SUGGESTED,
}

/**
 * Defines how many search categories there are.
 */
private const val NUMBER_OF_SEARCH_CATEGORIES = 3

@ActivityScoped
class Searcher @Inject constructor(
    @ActivityContext private val context: Context,
    private val appManager: AppManager,
    private val smartSearcher: SmartSearcher,
    private val filePreferenceManager: FilePreferenceManager,
) {
    val hasFinishedSearching: Boolean
        get() = numberOfLoadedCategories == NUMBER_OF_SEARCH_CATEGORIES

    private var webRequestCoroutine = CoroutineScope(Dispatchers.IO)
    private var appSearchCoroutine = CoroutineScope(Dispatchers.IO)
    private var fileSearchCoroutine = CoroutineScope(Dispatchers.IO)
    private var suggestedSearchCoroutine = CoroutineScope(Dispatchers.IO)
    private val resultCallbacks = mutableListOf<ResultCallback>()

    /**
     * Counts the number of search categories that have finished loading.
     * For example, if [Searcher] has finished searching through apps,
     * this counter is incremented by 1.
     */
    private var numberOfLoadedCategories = 0

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
        numberOfLoadedCategories = 0
    }

    fun requestSpecificSearch(category: SearchCategory, keyword: String): SearchResult {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return when (category) {
            SearchCategory.FILES -> SearchResult.Files(
                query = keyword,
                files = searchFiles(searchRegex),
            )
            SearchCategory.APPS -> SearchResult.Apps(
                query = keyword,
                apps = appManager.searchApps(searchRegex),
            )
            else -> SearchResult.None(keyword)
        }
    }

    /**
     * Cancels any pending search requests
     */
    fun cancelPendingSearch() {
        smartSearcher.cancelWebSearch()
        appSearchCoroutine.cancel()
        fileSearchCoroutine.cancel()
        webRequestCoroutine.cancel()
        suggestedSearchCoroutine.cancel()
        if (::searchTimer.isInitialized) {
            searchTimer.cancel()
        }
        numberOfLoadedCategories = 0
    }

    private fun performSearch(keyword: String) {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        appSearchCoroutine = CoroutineScope(Dispatchers.IO).also {
            it.launch {
                val result = withContext(Dispatchers.IO) {
                    appManager.searchApps(searchRegex)
                }

                numberOfLoadedCategories++
                if (result.isNotEmpty()) {
                    resultCallbacks.forEach {
                        it(SearchResult.Apps(keyword, apps = result))
                        it(SearchResult.Suggested.App(keyword, suggestedApp = result[0]))
                    }
                }
            }
        }

        fileSearchCoroutine = CoroutineScope(Dispatchers.IO).also {
            it.launch {
                val result = withContext(Dispatchers.IO) {
                    searchFiles(searchRegex)
                }

                numberOfLoadedCategories++
                resultCallbacks.forEach {
                    it(SearchResult.Files(keyword, files = result))
                }
            }
        }

        suggestedSearchCoroutine = CoroutineScope(Dispatchers.IO).also {
            it.launch {
                val result = withContext(Dispatchers.IO) {
                    smartSearcher.search(keyword)
                }

                numberOfLoadedCategories++
                resultCallbacks.forEach {
                    it(result)
                }
            }
        }
    }

    private fun searchFiles(searchRegex: Regex): List<DocumentFile>? {
        val paths = filePreferenceManager.includedPaths

        if (paths.size == 0) {
            return null
        }

        return paths
            .fold(listOf<DocumentFile>()) { allFiles, path ->
                if (path == "") return@fold allFiles
                val doc = DocumentFile.fromTreeUri(context, Uri.parse(path))
                if (doc != null) allFiles + getFilesRecursive(doc) else allFiles
            }
            .filter { it.name?.contains(searchRegex) ?: false }
            .sortedWith { file1, file2 ->
                compareStringsWithRegex(
                    file1.name!!,
                    file2.name!!,
                    searchRegex
                )
            }
    }

    private fun getFilesRecursive(root: DocumentFile): List<DocumentFile> {
        val files = mutableListOf<DocumentFile>()

        root.listFiles().forEach { file ->
            if (file.isDirectory) {
                files.addAll(getFilesRecursive(file))
            } else {
                files.add(file)
            }
        }

        return files
    }
}
