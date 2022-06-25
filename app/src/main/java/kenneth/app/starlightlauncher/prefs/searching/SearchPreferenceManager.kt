package kenneth.app.starlightlauncher.prefs.searching

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.InternalLauncherEvent
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.extension.Extension
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject
import javax.inject.Singleton

private const val CATEGORY_ORDER_LIST_SEPARATOR = ";"

/**
 * Represents an event that search preference has changed.
 * Subtypes of this class specifies exactly which search preference has changed.
 */
internal sealed class SearchPreferenceChanged : InternalLauncherEvent() {
    /**
     * The order of search category has changed.
     */
    data class SearchCategoryOrderChanged(
        /**
         * The original position of the search category moved by the user.
         */
        val fromIndex: Int,
        /**
         * The new position of the search category moved by the user.
         */
        val toIndex: Int
    ) :
        SearchPreferenceChanged()
}

/**
 * Manages search preferences for this launcher.
 */
@Singleton
internal class SearchPreferenceManager @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val extensionManager: ExtensionManager,
    private val launcherEventChannel: LauncherEventChannel,
    @ApplicationContext context: Context,
) {
    private val keys = SearchPreferencesPrefKeys(context)

    private var enabledSearchModules = mutableSetOf<String>()

    private var _categoryOrder = mutableListOf<String>()

    /**
     * Defines the order the search modules should appear on the search result page,
     * in an array of names of the search modules.
     */
    val categoryOrder
        get() = _categoryOrder as List<String>

    init {
        getSearchModuleOrder(extensionManager.installedExtensions)
        getEnabledSearchModules(extensionManager.installedExtensions)
    }

    /**
     * Returns the order the search module should appear on the search result list, or -1
     * if it doesn't exist.
     *
     * @param searchModuleName The name of the [SearchModule]
     */
    fun orderOf(searchModuleName: String) = categoryOrder.indexOf(searchModuleName)

    fun changeSearchCategoryOrder(fromIndex: Int, toIndex: Int, newOrder: List<String>) {
        _categoryOrder = newOrder.toMutableList()
        saveOrderList()
        launcherEventChannel.add(
            SearchPreferenceChanged.SearchCategoryOrderChanged(
                fromIndex,
                toIndex
            )
        )
    }

    private fun getSearchModuleOrder(extensions: Collection<Extension>) {
        // when the launcher is initializing, there may be new modules installed
        // since the last time the search module order list is saved to storage.
        //
        // therefore, we need to find out what modules are newly installed since last save,
        // add them to the order list and save the new list.
        //
        // SearchPreferenceManager will load the search module order list from storage
        // when initializing. we obtain the list, then update the list accordingly.

        val savedOrder = sharedPreferences.getString(keys.searchCategoryOrder, null)
            ?.split(CATEGORY_ORDER_LIST_SEPARATOR)

        if (savedOrder == null) {
            _categoryOrder += extensions.map { it.name }
        } else {
            _categoryOrder = savedOrder.toMutableList().also {
                it.retainAll { ext -> extensionManager.isExtensionInstalled(ext) }
            }

            // find newly-installed extensions
            extensions.forEach {
                if (savedOrder.indexOf(it.name) < 0) {
                    _categoryOrder += it.name
                }
            }

            saveOrderList()
        }
    }

    private fun getEnabledSearchModules(extensions: Collection<Extension>) {
        val saved = sharedPreferences.getStringSet(keys.enabledSearchModules, null)
        if (saved == null) {
            enabledSearchModules.addAll(extensions.map { it.name })
        } else {
            enabledSearchModules += saved
            enabledSearchModules.retainAll { extensionManager.isExtensionInstalled(it) }

            // find uninstalled extensions
            extensions.forEach {
                if (!saved.contains(it.name)) {
                    enabledSearchModules += it.name
                }
            }

            saveEnabledSearchModules()
        }
    }

    private fun saveEnabledSearchModules() {
        sharedPreferences.edit(commit = true) {
            putStringSet(keys.enabledSearchModules, enabledSearchModules)
        }
    }

    private fun saveOrderList() {
        sharedPreferences.edit(commit = true) {
            putString(
                keys.searchCategoryOrder,
                _categoryOrder.joinToString(CATEGORY_ORDER_LIST_SEPARATOR)
            )
        }
    }
}

private class SearchPreferencesPrefKeys(context: Context) {
    val searchCategoryOrder by lazy { context.getString(R.string.pref_key_search_category_order) }

    val enabledSearchModules by lazy { context.getString(R.string.search_enabled_modules_pref_key) }
}
