package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.keelar.exprk.Expressions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.api.DuckDuckGoApi
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.Comparator
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 500

typealias ResultCallback = (Searcher.Result, SearchType) -> Unit

/**
 * Different types that Searcher will search for
 */
enum class SearchType {
    ALL, APPS, FILES,
}

@Module
@InstallIn(ActivityComponent::class)
object SearcherModule {
    @Provides
    fun provideSmartSearcher(expressionParser: Expressions, duckduckgoApiClient: DuckDuckGoApi) =
        SmartSearcher(expressionParser, duckduckgoApiClient)
}

class Searcher @Inject constructor(
    @ActivityContext private val context: Context,
    private val smartSearcher: SmartSearcher,
    private val filePreferenceManager: FilePreferenceManager,
    private val locale: Locale
) {
    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    private val webRequestCoroutine = CoroutineScope(Dispatchers.IO)

    private lateinit var searchTimer: TimerTask
    private lateinit var appList: List<ResolveInfo>
    private lateinit var resultCallback: ResultCallback

    /**
     * Adds a listener that is called when search result is available.
     */
    fun setSearchResultListener(callback: ResultCallback) {
        resultCallback = callback
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
            webRequestCoroutine.launch {
                smartSearcher.performWebSearch(keyword)
            }
            webRequestCoroutine.launch {
                resultCallback(performSearch(keyword.toLowerCase(locale)), SearchType.ALL)
            }
        }
    }

    fun requestSpecificSearch(type: SearchType, keyword: String): Result {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return when (type) {
            SearchType.FILES -> Result(
                query = keyword,
                files = searchFiles(searchRegex),
            )
            SearchType.APPS -> Result(
                query = keyword,
                apps = searchApps(searchRegex),
            )
            else -> Result(query = keyword)
        }
    }

    /**
     * Cancels any pending search requests
     */
    fun cancelPendingSearch() {
        smartSearcher.cancelWebSearch()
        searchTimer.cancel()
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

    private suspend fun performSearch(keyword: String): Result {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return Result(
            query = keyword,
            apps = withContext(Dispatchers.IO) {
                searchApps(searchRegex)
            },
            files = withContext(Dispatchers.IO) {
                searchFiles(searchRegex)
            },
            suggested = smartSearcher.search(keyword),
        )
    }

    private fun searchApps(searchRegex: Regex) = appList
        .filter { it.loadLabel(context.packageManager).contains(searchRegex) }
        .sortedWith(appRanker(searchRegex))

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

    /**
     * appRanker ranks apps in the list based on the search query.
     */
    private fun appRanker(searchRegex: Regex): Comparator<ResolveInfo> {
        return Comparator { app1, app2 ->
            val appName1 = app1.loadLabel(context.packageManager)
            val appName2 = app2.loadLabel(context.packageManager)

            compareStringsWithRegex(appName1.toString(), appName2.toString(), searchRegex)
        }
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

private fun compareStringsWithRegex(string1: String, string2: String, regex: Regex): Int {
    val result1 = regex.findAll(string1).toList()
    val result2 = regex.findAll(string2).toList()

    // first, find the longest match in all matches
    // if the query has a longer match of the name of the first app than the second app
    // the first app should come first

    val result1LongestMatch = result1.foldIndexed(0) { i, len, result ->
        when {
            i == 0 -> len + 1
            result.range.first - result1[i - 1].range.first > 1 -> 1
            else -> len + 1
        }
    }

    val result2LongestMatch = result2.foldIndexed(0) { i, len, result ->
        when {
            i == 0 -> len + 1
            result.range.first - result2[i - 1].range.first > 1 -> 1
            else -> len + 1
        }
    }

    if (result1LongestMatch != result2LongestMatch) {
        return result2LongestMatch - result1LongestMatch
    }

    // if the longest matches have the same length
    // then find which match comes first
    // for example, if the query is "g", string1 is "google", and string2 is "settings"
    // app1 should come first because the "g" in google comes first

    val result1FirstMatchIndex = result1[0].range.first
    val result2FirstMatchIndex = result2[0].range.first

    return result1FirstMatchIndex - result2FirstMatchIndex
}
