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

    private lateinit var launcherContext: Context
    private lateinit var preferences: AppSearchModulePreferences
    private lateinit var launcherApps: LauncherApps
    private lateinit var userManager: UserManager
    private var defaultUserNo: Long = -1

    /**
     * Stores all apps installed on the device.
     */
    private val allApps = mutableListOf<LauncherActivityInfo>()

    /**
     * Maps app package names to their corresponding labels.
     */
    private val appLabels = mutableMapOf<String, String>()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return
            allApps.removeAll { it.applicationInfo.packageName == packageName && it.user == user }
            appLabels.remove(packageName)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            allApps.addAll(activities)
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            allApps.apply {
                removeAll { it.applicationInfo.packageName == packageName && it.user == user }
                addAll(activities)
            }
            appLabels.remove(packageName)
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
                allApps.apply {
                    removeAll { it.applicationInfo.packageName == packageName && it.user == user }
                    addAll(activities)
                }
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

            allApps.removeAll { it.user == user && packageNames.contains(it.applicationInfo.packageName) }
            packageNames.forEach { appLabels.remove(it) }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null) return
            when (intent?.action) {
                Intent.ACTION_MANAGED_PROFILE_ADDED -> {
                    val user =
                        if (Build.VERSION.SDK_INT >= 33)
                            intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
                        else
                            UserHandle(intent.getParcelableExtra(Intent.EXTRA_USER))

                    user?.let { loadAppsForUser(it) }
                }
                Intent.ACTION_MANAGED_PROFILE_REMOVED -> {
                    val removedUser =
                        if (Build.VERSION.SDK_INT >= 33)
                            intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
                        else
                            UserHandle(intent.getParcelableExtra(Intent.EXTRA_USER))

                    allApps.removeAll { it.user == removedUser }
                }
            }
        }
    }

    override fun initialize(launcher: StarlightLauncherApi) {
        launcherContext = launcher.context
        launcherApps =
            launcher.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = launcher.context.getSystemService(Context.USER_SERVICE) as UserManager
        adapter = AppSearchResultAdapter(launcher.context, launcher)
        preferences = AppSearchModulePreferences.getInstance(launcher.context)
        defaultUserNo = userManager.getSerialNumberForUser(Process.myUserHandle())

        context.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        })
        loadApps()
    }

    override fun cleanup() {
        launcherApps.unregisterCallback(launcherAppsCallback)
        context.unregisterReceiver(broadcastReceiver)
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult = allApps
        .filter { app -> appLabels[app.applicationInfo.packageName]?.contains(keywordRegex) == true }
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
                            val appName = appLabels[it.applicationInfo.packageName]!!
                            fuzzyScore(appName, keyword, locale)
                        }
                        .take(20)
                )
            }
        }

    /**
     * Finds apps for all user profiles.
     */
    private fun loadApps() {
        userManager.userProfiles.forEach { loadAppsForUser(it) }
        launcherApps.registerCallback(launcherAppsCallback)
    }

    /**
     * Finds apps installed under [user].
     */
    private fun loadAppsForUser(user: UserHandle) {
        launcherApps.getActivityList(null, user)
            .forEach {
                val packageName = it.applicationInfo.packageName
                allApps += it
                appLabels[packageName] = it.label.toString()
            }
    }

    class Result(query: String, val apps: AppList) : SearchResult(query, EXTENSION_NAME)
}