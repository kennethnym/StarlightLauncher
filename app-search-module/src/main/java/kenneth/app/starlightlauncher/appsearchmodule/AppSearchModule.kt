package kenneth.app.starlightlauncher.appsearchmodule

import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.utils.sortByRegex

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appsearchmodule"

typealias AppList = List<ActivityInfo>

class AppSearchModule : BroadcastReceiver(), SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override val adapter
        get() = searchResultAdapter

    private lateinit var searchResultAdapter: AppSearchResultAdapter
    private lateinit var mainContext: Context
    private lateinit var launcherContext: Context

    private val currentAppList = mutableListOf<ActivityInfo>()
    private val appLabels = mutableMapOf<String, String>()

    private lateinit var preferences: AppSearchModulePreferences

    internal val context
        get() = mainContext

    override fun initialize(launcher: StarlightLauncherApi) {
        launcherContext = launcher.context
        mainContext = launcherContext
        searchResultAdapter = AppSearchResultAdapter(mainContext, launcher)
        preferences = AppSearchModulePreferences.getInstance(context)

        metadata = SearchModule.Metadata(
            extensionName = EXTENSION_NAME,
            displayName = mainContext.getString(R.string.app_search_module_display_name),
            description = mainContext.getString(R.string.app_search_module_description),
        )

        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        launcherContext.packageManager.queryIntentActivities(mainIntent, 0)
            .filter { (it.activityInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 1 }
            .forEach {
                val packageName = it.activityInfo.packageName
                val label =
                    it.activityInfo.applicationInfo.loadLabel(launcherContext.packageManager)
                        .toString()
                currentAppList.add(it.activityInfo)
                appLabels[packageName] = label
            }

        launcherContext.registerReceiver(this, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
    }

    override fun cleanup() {
        launcherContext.unregisterReceiver(this)
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): Result =
        Result(
            query = keyword,
            apps = currentAppList
                .filter { app -> appLabels[app.packageName]?.contains(keywordRegex) == true }
                .sortedWith { app1, app2 ->
                    val appName1 = appLabels[app1.packageName]!!
                    val appName2 = appLabels[app2.packageName]!!
                    return@sortedWith sortByRegex(appName1, appName2, keywordRegex)
                }
        )

    override fun onReceive(context: Context?, intent: Intent?) {
        val receivedPackageName = intent?.data?.schemeSpecificPart
        when (intent?.action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                currentAppList.removeAt(
                    currentAppList.indexOfFirst {
                        it.packageName == receivedPackageName
                    }
                )
            }
            Intent.ACTION_PACKAGE_ADDED -> {
                val packageLauncherIntent = Intent().apply {
                    `package` = receivedPackageName
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                context?.packageManager
                    ?.resolveActivity(packageLauncherIntent, 0)
                    ?.let { currentAppList.add(it.activityInfo) }
            }
        }
    }

    class Result(query: String, val apps: AppList) : SearchResult(query, EXTENSION_NAME)
}