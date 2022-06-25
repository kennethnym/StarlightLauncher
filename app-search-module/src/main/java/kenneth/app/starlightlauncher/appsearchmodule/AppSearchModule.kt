package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.sortByRegex
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appsearchmodule"

typealias AppList = List<LauncherActivityInfo>

class AppSearchModule(context: Context) : SearchModule(context) {
    override val metadata: Metadata = Metadata(
        extensionName = context.getString(R.string.app_search_module_name),
        displayName = context.getString(R.string.app_search_module_display_name),
        description = context.getString(R.string.app_search_module_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    private lateinit var launcherContext: Context
    private lateinit var preferences: AppSearchModulePreferences
    private lateinit var launcherApps: LauncherApps

    private val currentAppList = mutableListOf<LauncherActivityInfo>()

    /**
     * Maps app package names to their corresponding labels.
     */
    private val appLabels = mutableMapOf<String, String>()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            currentAppList.removeAll { it.applicationInfo.packageName == packageName }
            appLabels.remove(packageName)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            currentAppList += activities
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            currentAppList.removeAll { it.componentName.packageName == packageName }
            appLabels.remove(packageName)
            currentAppList += activities
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (packageNames == null || user == null) return

            packageNames.forEach { packageName ->
                val activities = launcherApps.getActivityList(packageName, user)
                currentAppList.removeAll { it.applicationInfo.packageName == packageName }
                currentAppList += activities
                activities.forEach {
                    appLabels[it.applicationInfo.packageName] = it.label.toString()
                }
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (packageNames == null || user == null) return

            currentAppList.removeAll { packageNames.contains(it.componentName.packageName) }
            packageNames.forEach { appLabels.remove(it) }
        }
    }

    override fun initialize(launcher: StarlightLauncherApi) {
        launcherContext = launcher.context
        launcherApps =
            launcher.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        adapter = AppSearchResultAdapter(launcher.context, launcher)
        preferences = AppSearchModulePreferences.getInstance(launcher.context)

        with(launcherApps) {
            getActivityList(null, Process.myUserHandle())
                .forEach {
                    val packageName = it.applicationInfo.packageName
                    currentAppList.add(it)
                    appLabels[packageName] = it.label.toString()
                }

            registerCallback(launcherAppsCallback)
        }
    }

    override fun cleanup() {
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

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
}