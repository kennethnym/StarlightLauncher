package kenneth.app.starlightlauncher.appsearchmodule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.fuzzyScore
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appsearchmodule"

typealias AppList = List<LauncherActivityInfo>

// TODO: use PackageManager.getUserBadgedIcon to get work profile badge

class AppSearchModule(context: Context) : SearchModule(context) {
    override val metadata: Metadata = Metadata(
        extensionName = context.getString(R.string.app_search_module_name),
        displayName = context.getString(R.string.app_search_module_display_name),
        description = context.getString(R.string.app_search_module_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    private lateinit var launcher: StarlightLauncherApi
    private lateinit var launcherContext: Context
    private lateinit var preferences: AppSearchModulePreferences
    private lateinit var userManager: UserManager
    private var defaultUserNo: Long = -1

    override fun initialize(launcher: StarlightLauncherApi) {
        this.launcher = launcher
        launcherContext = launcher.context
        userManager = launcher.context.getSystemService(Context.USER_SERVICE) as UserManager
        adapter = AppSearchResultAdapter(launcher.context, launcher)
        preferences = AppSearchModulePreferences.getInstance(launcher.context)
        defaultUserNo = userManager.getSerialNumberForUser(Process.myUserHandle())
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult = launcher
        .installedApps
        .filter { app ->
            launcher.appLabelOf(app.applicationInfo.packageName)?.contains(keywordRegex) == true
        }
        .let {
            if (it.isEmpty())
                SearchResult.None(keyword, EXTENSION_NAME)
            else {
                val locale =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        Resources.getSystem().configuration.locales.get(0)
                    else
                        Resources.getSystem().configuration.locale

                Result(
                    query = keyword,
                    apps = it
                        .sortedByDescending {
                            val appName = launcher.appLabelOf(it.applicationInfo.packageName)!!
                            fuzzyScore(appName, keyword, locale)
                        }
                        .take(20)
                )
            }
        }

    class Result(query: String, val apps: AppList) : SearchResult(query, EXTENSION_NAME)
}