package kenneth.app.spotlightlauncher

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.util.*
import kotlin.concurrent.schedule

private const val SEARCH_DELAY: Long = 1000

class Searcher(private val packageManager: PackageManager) {
    private val locale = Locale.getDefault()

    private lateinit var searchTimer: TimerTask
    private lateinit var appList: List<PackageInfo>
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
        appList = packageManager
            .getInstalledPackages(0)
//            .filter { notSystemApps(it) }
    }

    private fun notSystemApps(packageInfo: PackageInfo): Boolean =
        packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0

    private fun performSearch(keyword: String): Result {
        val searchRegex = Regex("[$keyword]", RegexOption.IGNORE_CASE)

        return Result(
            apps = appList
                .filter { packageInfo ->
                    packageInfo.applicationInfo.loadLabel(packageManager).contains(searchRegex)
                }
                .sortedWith({ app1, app2 ->
                    val app1Name = app1.applicationInfo.loadLabel(packageManager)
                    val app2Name = app2.applicationInfo.loadLabel(packageManager)
                    val result1 = searchRegex.findAll(app1Name).toList()
                    val result2 = searchRegex.findAll(app2Name).toList()

                    result1[0].range.first - result2[0].range.first
                })
        )
    }

    data class Result(val apps: List<PackageInfo>)
}
