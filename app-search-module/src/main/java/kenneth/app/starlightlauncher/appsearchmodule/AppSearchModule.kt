package kenneth.app.starlightlauncher.appsearchmodule

import android.content.*
import android.content.pm.*
import android.os.Process
import android.os.UserHandle
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.utils.sortByRegex

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appsearchmodule"

typealias AppList = List<LauncherActivityInfo>

class AppSearchModule : LauncherApps.Callback(), SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override val adapter
        get() = searchResultAdapter

    private lateinit var searchResultAdapter: AppSearchResultAdapter
    private lateinit var mainContext: Context
    private lateinit var launcherContext: Context
    private lateinit var preferences: AppSearchModulePreferences
    private lateinit var launcherApps: LauncherApps

    private val currentAppList = mutableListOf<LauncherActivityInfo>()
    private val appLabels = mutableMapOf<String, String>()

    internal val context
        get() = mainContext

    override fun initialize(launcher: StarlightLauncherApi) {
        launcherContext = launcher.context
        mainContext = launcherContext
        launcherApps = mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        searchResultAdapter = AppSearchResultAdapter(mainContext, launcher)
        preferences = AppSearchModulePreferences.getInstance(context)

        metadata = SearchModule.Metadata(
            extensionName = EXTENSION_NAME,
            displayName = mainContext.getString(R.string.app_search_module_display_name),
            description = mainContext.getString(R.string.app_search_module_description),
        )

        launcherApps.getActivityList(null, Process.myUserHandle())
            .forEach {
                val packageName = it.applicationInfo.packageName
                currentAppList.add(it)
                appLabels[packageName] = it.label.toString()
            }
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        currentAppList
            .filter { app -> appLabels[app.applicationInfo.packageName]?.contains(keywordRegex) == true }
            .let {
                if (it.isEmpty())
                    SearchResult.None(keyword, EXTENSION_NAME)
                else
                    Result(
                        query = keyword,
                        apps = it.sortedWith { app1, app2 ->
                            val appName1 = appLabels[app1.applicationInfo.packageName]!!
                            val appName2 = appLabels[app2.applicationInfo.packageName]!!
                            return@sortedWith sortByRegex(appName1, appName2, keywordRegex)
                        }
                    )
            }

    class Result(query: String, val apps: AppList) : SearchResult(query, EXTENSION_NAME)

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
        if (packageName == null || user == null) return

        currentAppList.removeAll { it.applicationInfo.packageName == packageName }
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
        if (packageName == null || user == null) return

        currentAppList += launcherApps.getActivityList(packageName, user)
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {}

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) {
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) {
    }
}