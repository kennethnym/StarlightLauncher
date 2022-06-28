package kenneth.app.starlightlauncher.appsearchmodule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.sortByRegex
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
     * Stores all apps installed on the device,
     * indexed by the serial number of the user profile the apps are installed under.
     */
    private val allApps = mutableMapOf<Long, MutableList<LauncherActivityInfo>>()

    /**
     * Maps app package names to their corresponding labels.
     */
    private val appLabels = mutableMapOf<String, String>()

    /**
     * Maps user labels to their corresponding serial number.
     */
    private val userLabelsToSerialNumbers = mutableMapOf<String, Long>()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val userNo = userManager.getSerialNumberForUser(user)

            allApps[userNo]?.removeAll { it.applicationInfo.packageName == packageName }
            appLabels.remove(packageName)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val userNo = userManager.getSerialNumberForUser(user)

            val activities = launcherApps.getActivityList(packageName, user)
            allApps[userNo]?.addAll(activities)
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val userNo = userManager.getSerialNumberForUser(user)
            val activities = launcherApps.getActivityList(packageName, user)
            allApps[userNo]?.let { apps ->
                apps.removeAll { it.componentName.packageName == packageName }
                apps.addAll(activities)
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

            val userNo = userManager.getSerialNumberForUser(user)

            packageNames.forEach { packageName ->
                val activities = launcherApps.getActivityList(packageName, user)
                allApps[userNo]?.let { apps ->
                    apps.removeAll { it.applicationInfo.packageName == packageName }
                    apps.addAll(activities)
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

            val userNo = userManager.getSerialNumberForUser(user)

            allApps[userNo]?.removeAll { packageNames.contains(it.componentName.packageName) }
            packageNames.forEach { appLabels.remove(it) }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

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

        loadApps()
    }

    override fun cleanup() {
        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        when {
            keyword.startsWith('.') -> {
                // search for apps in other user profiles
                // e.g. ".work gmail" searches for gmail in work profile
                val split = keyword.split(' ')
                if (split.isEmpty() || split.size == 1) {
                    // syntax is not valid, search in app list of default user
                    allApps[defaultUserNo]
                } else {
                    // if keyword is ".my work profile gmail"
                    // profileName will be "my work profile"
                    val profileName =
                        (listOf(split.first().substring(1)) + split.subList(
                            1,
                            split.lastIndex
                        )).joinToString(" ")
                    userLabelsToSerialNumbers[profileName]?.let {
                        // user found, search in app list of the user profile
                        allApps[it]
                    }
                        ?: allApps[defaultUserNo] // user not found, search in app list of default user
                }
            }
            else -> allApps[defaultUserNo]
        }
            ?.filter { app -> appLabels[app.applicationInfo.packageName]?.contains(keywordRegex) == true }
            ?.let {
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
            ?: SearchResult.None(keyword, EXTENSION_NAME)

    private fun loadApps() {
        with(launcherApps) {
            userManager.userProfiles.forEach { user ->
                val userNo = userManager.getSerialNumberForUser(user)
                val userLabel =
                    launcherContext.packageManager.getUserBadgedLabel("", user).toString()
                        .lowercase()
                val userApps = mutableListOf<LauncherActivityInfo>()
                getActivityList(null, user)
                    .forEach {
                        val packageName = it.applicationInfo.packageName
                        userApps.add(it)
                        appLabels[packageName] = it.label.toString()
                    }
                userLabelsToSerialNumbers[userLabel] = userNo
                allApps[userNo] = userApps
            }

            registerCallback(launcherAppsCallback)
        }
    }

    class Result(query: String, val apps: AppList) : SearchResult(query, EXTENSION_NAME)
}