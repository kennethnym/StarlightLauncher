package kenneth.app.spotlightlauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import java.util.*
import kotlin.Comparator
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 1000

class Searcher(private val packageManager: PackageManager) {
    private val locale = Locale.getDefault()
    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    private lateinit var searchTimer: TimerTask
    private lateinit var appList: List<ResolveInfo>
    private lateinit var resultCallback: (Result) -> Unit

    fun setSearchResultListener(callback: (Result) -> Unit) {
        resultCallback = callback
    }

    fun requestSearch(keyword: String) {
        if (::searchTimer.isInitialized) cancelPendingSearch()

        searchTimer = Timer().schedule(SEARCH_DELAY) {
            resultCallback(performSearch(keyword.toLowerCase(locale)))
        }
    }

    fun cancelPendingSearch() = searchTimer.cancel()

    fun refreshAppList() {
        appList = packageManager.queryIntentActivities(mainIntent, 0).filter { notSystemApps(it) }
    }

    private fun notSystemApps(appInfo: ResolveInfo): Boolean =
        (appInfo.activityInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 1

    private fun performSearch(keyword: String): Result {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return Result(
            apps = appList
                .filter { app ->
                    app.loadLabel(packageManager).contains(searchRegex)
                }
                .sortedWith(appRanker(keyword))
        )
    }

    /**
     * appRanker ranks apps in the list based on the search query.
     */
    private fun appRanker(searchQuery: String): Comparator<ResolveInfo> {
        val searchRegex = Regex("[$searchQuery]", RegexOption.IGNORE_CASE)

        return Comparator { app1, app2 ->
            val appName1 = app1.loadLabel(packageManager)
            val appName2 = app2.loadLabel(packageManager)

            val result1 = searchRegex.findAll(appName1).toList()
            val result2 = searchRegex.findAll(appName2).toList()

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
                return@Comparator result2LongestMatch - result1LongestMatch
            }

            val result1FirstMatchIndex = result1[0].range.first
            val result2FirstMatchIndex = result2[0].range.first

            return@Comparator result1FirstMatchIndex - result2FirstMatchIndex
        }
    }

    data class Result(val apps: List<ResolveInfo>)
}
