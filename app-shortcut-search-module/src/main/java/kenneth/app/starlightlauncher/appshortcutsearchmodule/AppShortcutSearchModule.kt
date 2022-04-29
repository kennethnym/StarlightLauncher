package kenneth.app.starlightlauncher.appshortcutsearchmodule

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.annotation.RequiresApi
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.utils.sortByRegex
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.appshortcutsearchmodule"

class AppShortcutSearchModule : SearchModule, LauncherApps.Callback() {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override lateinit var adapter: SearchResultAdapter
        private set

    /**
     * App shortcuts grouped by their package names.
     * Each shortcut in a group is keyed by the ID of the shortcut.
     */
    private val shortcutsIndexed = mutableMapOf<String, MutableMap<String, ShortcutInfo>>()

    private val shortcutList = mutableListOf<ShortcutInfo>()

    private lateinit var launcherApps: LauncherApps

    override fun initialize(launcher: StarlightLauncherApi) {
        val mainContext = launcher.context

        metadata = SearchModule.Metadata(
            extensionName = mainContext.getString(R.string.app_shortcut_search_module_name),
            displayName = mainContext.getString(R.string.app_shortcut_search_module_display_name),
            description = mainContext.getString(R.string.app_shortcut_search_module_description),
        )

        adapter = AppShortcutSearchResultAdapter(mainContext, launcher)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            launcherApps =
                mainContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

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
                        ?.let {
                            shortcutList += it
                            it.forEach { shortcut ->
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
                }

                registerCallback(this@AppShortcutSearchModule)
            }
        }
    }

    override fun cleanup() {
        launcherApps.unregisterCallback(this)
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || shortcutsIndexed.isEmpty())
            SearchResult.None(keyword, EXTENSION_NAME)
        else
            Result(
                query = keyword,
                shortcuts = shortcutList
                    .sortedWith { shortcut1, shortcut2 ->
                        val name1 = shortcut1.longLabel ?: shortcut1.shortLabel ?: ""
                        val name2 = shortcut2.longLabel ?: shortcut2.shortLabel ?: ""
                        return@sortedWith sortByRegex(
                            name1.toString(),
                            name2.toString(),
                            keywordRegex
                        )
                    }
                    .take(2)
            )

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

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {

    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
    }

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

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun updateShortcutInfo(packageName: String?, newShortcuts: MutableList<ShortcutInfo>) {
        val currentShortcuts = this.shortcutsIndexed[packageName] ?: return
        // find all newly added shortcuts
        val addedShortcuts =
            newShortcuts.filter { !currentShortcuts.containsKey(it.id) }

        val updatedShortcutIds = mutableListOf<String>()

        currentShortcuts.forEach { (currentShortcutId, currentShortcut) ->
            if (!currentShortcut.isPinned) {
                val newShortcutInfo = newShortcuts.find { it.id == currentShortcutId }

                when {
                    newShortcutInfo == null -> currentShortcuts.remove(currentShortcutId)

                    newShortcutInfo.lastChangedTimestamp > currentShortcut.lastChangedTimestamp ->
                        updatedShortcutIds += currentShortcutId
                }
            }
        }

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

    internal class Result(query: String, internal val shortcuts: List<ShortcutInfo>) :
        SearchResult(query, EXTENSION_NAME)
}