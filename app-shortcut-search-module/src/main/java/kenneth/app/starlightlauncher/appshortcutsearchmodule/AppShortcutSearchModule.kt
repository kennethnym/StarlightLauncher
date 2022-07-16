package kenneth.app.starlightlauncher.appshortcutsearchmodule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.fuzzyScore
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appshortcutsearchmodule"

@RequiresApi(Build.VERSION_CODES.N_MR1)
class AppShortcutSearchModule(context: Context) : SearchModule(context), DefaultLifecycleObserver {
    override val metadata = Metadata(
        extensionName = context.getString(R.string.app_shortcut_search_module_name),
        displayName = context.getString(R.string.app_shortcut_search_module_display_name),
        description = context.getString(R.string.app_shortcut_search_module_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    /**
     * App shortcuts grouped by their package names.
     * Each shortcut in a group is keyed by the ID of the shortcut.
     */
    private val shortcutsIndexed = mutableMapOf<String, MutableMap<String, ShortcutInfo>>()

    private val shortcutList = mutableListOf<AppShortcut>()

    private var hasNoPermission = false

    private lateinit var launcherApps: LauncherApps
    private lateinit var launcher: StarlightLauncherApi

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onShortcutsChanged(
            packageName: String,
            newShortcuts: MutableList<ShortcutInfo>,
            user: UserHandle
        ) {
            super.onShortcutsChanged(packageName, newShortcuts, user)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                updateShortcutInfo(packageName, newShortcuts)
            }
        }

        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {}

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {}

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

    override fun initialize(launcher: StarlightLauncherApi) {
        val mainContext = launcher.context

        adapter = AppShortcutSearchResultAdapter(mainContext, launcher)
        launcherApps =
            mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        this.launcher = launcher
        if (mainContext is AppCompatActivity) {
            mainContext.lifecycle.addObserver(this)
        }

        queryShortcuts()
    }

    override fun cleanup() {
        launcherApps.unregisterCallback(launcherAppsCallback)
        launcher.context.let {
            if (it is AppCompatActivity)
                it.lifecycle.removeObserver(this)
        }
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        if (shortcutsIndexed.isEmpty())
            SearchResult.None(keyword, EXTENSION_NAME)
        else {
            val locale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Resources.getSystem().configuration.locales.get(0)
                else
                    Resources.getSystem().configuration.locale

            Result(
                query = keyword,
                shortcuts = shortcutList
                    .sortedByDescending {
                        val name = it.info.longLabel ?: it.info.shortLabel ?: ""
                        fuzzyScore(name, keyword, locale)
                    }
                    .take(2)
            )
        }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            checkIsDefaultLauncher()
        }
    }

    private fun checkIsDefaultLauncher() {
        val resolvedDefaultHome =
            context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }, 0)

        // check if launcher is set as default and if this module previously has no permission to query shortcuts
        // if so, query shortcuts
        if (resolvedDefaultHome?.activityInfo?.packageName == context.packageName && hasNoPermission) {
            queryShortcuts()
        }
    }

    private fun queryShortcuts() {
        with(launcherApps) {
            try {
                getShortcuts(
                    LauncherApps.ShortcutQuery().apply {
                        setQueryFlags(
                            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                        )
                    },
                    Process.myUserHandle()
                )
                    ?.let { shortcuts ->
                        shortcutList += shortcuts.map { info ->
                            AppShortcut(
                                info,
                                app = getActivityList(info.`package`, Process.myUserHandle())
                                    .find { it.componentName == info.activity }
                            )
                        }
                        shortcuts.forEach { shortcut ->
                            if (shortcutsIndexed.contains(shortcut.`package`)) {
                                shortcutsIndexed[shortcut.`package`]?.set(shortcut.id, shortcut)
                            } else {
                                shortcutsIndexed[shortcut.`package`] = mutableMapOf(
                                    shortcut.id to shortcut
                                )
                            }
                        }
                    }
            } catch (ex: SecurityException) {
                // not the default launcher app
                // cannot fetch shortcut infos
                hasNoPermission = true
            }

            registerCallback(launcherAppsCallback)
        }
    }

    private fun updateShortcutInfo(packageName: String?, newShortcuts: MutableList<ShortcutInfo>) {
        val currentShortcuts = this.shortcutsIndexed[packageName] ?: return
        // find all newly added shortcuts
        val addedShortcuts =
            newShortcuts.filter { !currentShortcuts.containsKey(it.id) }

        val updatedShortcutIds = mutableListOf<String>()
        val removedShortcutIds = mutableSetOf<String>()

        currentShortcuts.forEach { (currentShortcutId, currentShortcut) ->
            if (!currentShortcut.isPinned) {
                val newShortcutInfo = newShortcuts.find { it.id == currentShortcutId }

                when {
                    newShortcutInfo == null -> removedShortcutIds += currentShortcutId

                    newShortcutInfo.lastChangedTimestamp > currentShortcut.lastChangedTimestamp ->
                        updatedShortcutIds += currentShortcutId
                }
            }
        }

        currentShortcuts.entries.removeAll { removedShortcutIds.contains(it.key) }

        val shortcutQuery = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setShortcutIds(updatedShortcutIds)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
            )

        launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())
            ?.forEach {
                currentShortcuts[it.id] = it
            }

        addedShortcuts.forEach { currentShortcuts[it.id] = it }
    }

    internal class Result(query: String, internal val shortcuts: List<AppShortcut>) :
        SearchResult(query, EXTENSION_NAME)
}