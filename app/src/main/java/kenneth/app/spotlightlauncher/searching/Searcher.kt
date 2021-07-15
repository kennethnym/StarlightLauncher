package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.icu.number.NumberRangeFormatter
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.keelar.exprk.Expressions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.spotlightlauncher.api.DuckDuckGoApi
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 500

typealias ResultCallback = (Searcher.Result, SearchCategory) -> Unit

/**
 * Different categories that Searcher will search for
 */
enum class SearchCategory {
    ALL, APPS, FILES, SUGGESTED,
}

/**
 * Defines how many search categories there are.
 */
private val NUMBER_OF_SEARCH_CATEGORIES = 3

@Module
@InstallIn(ActivityComponent::class)
object SearcherModule {
    @Provides
    fun provideSmartSearcher(expressionParser: Expressions, duckduckgoApiClient: DuckDuckGoApi) =
        SmartSearcher(expressionParser, duckduckgoApiClient)

    @Provides
    fun provideAppSearcher(@ActivityContext context: Context) = AppSearcher(context)
}

@ActivityScoped
class Searcher @Inject constructor(
    @ActivityContext private val context: Context,
    private val appSearcher: AppSearcher,
    private val smartSearcher: SmartSearcher,
    private val filePreferenceManager: FilePreferenceManager,
) {
    val hasFinishedSearching: Boolean
        get() = numberOfLoadedCategories == NUMBER_OF_SEARCH_CATEGORIES

    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
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
    private lateinit var appList: List<ResolveInfo>

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

    fun requestSpecificSearch(category: SearchCategory, keyword: String): Result {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return when (category) {
            SearchCategory.FILES -> Result(
                query = keyword,
                files = searchFiles(searchRegex),
            )
            SearchCategory.APPS -> Result(
                query = keyword,
                apps = appSearcher.searchApps(searchRegex),
            )
            else -> Result(query = keyword)
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

    /**
     * Reloads the list of apps.
     */
    fun refreshAppList() {
        appList =
            context.packageManager.queryIntentActivities(mainIntent, 0).filter { notSystemApps(it) }
    }

    private fun notSystemApps(appInfo: ResolveInfo): Boolean =
        (appInfo.activityInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 1

    private fun performSearch(keyword: String) {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        appSearchCoroutine = CoroutineScope(Dispatchers.IO).also {
            it.launch {
                val result = withContext(Dispatchers.IO) {
                    appSearcher.searchApps(searchRegex)
                }

                numberOfLoadedCategories++
                resultCallbacks.forEach { cb ->
                    CoroutineScope(Dispatchers.Default).launch {
                        cb(
                            Result(keyword, apps = result),
                            SearchCategory.APPS,
                        )
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
                resultCallbacks.forEach { cb ->
                    CoroutineScope(Dispatchers.Default).launch {
                        cb(
                            Result(keyword, files = result),
                            SearchCategory.FILES,
                        )
                    }
                }
            }
        }

        suggestedSearchCoroutine = CoroutineScope(Dispatchers.IO).also {
            it.launch {
                val result = withContext(Dispatchers.IO) {
                    smartSearcher.search(keyword)
                }

                numberOfLoadedCategories++
                resultCallbacks.forEach { cb ->
                    CoroutineScope(Dispatchers.Default).launch {
                        cb(
                            Result(keyword, suggested = result),
                            SearchCategory.SUGGESTED,
                        )
                    }
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

    data class Result(
        val query: String,
        val apps: List<ResolveInfo> = emptyList(),
        val files: List<DocumentFile>? = null,
        val suggested: SmartSearcher.SuggestedResult = SmartSearcher.SuggestedResult(
            query,
            type = SuggestedResultType.NONE,
        )
    )
}
