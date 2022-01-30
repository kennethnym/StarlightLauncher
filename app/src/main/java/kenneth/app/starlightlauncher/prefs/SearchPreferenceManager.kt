package kenneth.app.starlightlauncher.prefs

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject
import javax.inject.Singleton

private const val CATEGORY_ORDER_LIST_SEPARATOR = ";"

@Singleton
class SearchPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val extensionManager: ExtensionManager,
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val enabledSearchModulesPrefKey =
        context.getString(R.string.search_enabled_modules_pref_key)

    private val searchCategoryOrderPrefKey =
        context.getString(R.string.search_category_order_pref_key)

    private val _enabledSearchModules =
        sharedPreferences.getStringSet(enabledSearchModulesPrefKey, null)
            ?.toMutableSet()
            ?: mutableSetOf<String>().apply {
                extensionManager.installedSearchModules.forEach {
                    add(it.metadata.extensionName)
                }
            }

    /**
     * Defines the order the search modules should appear on the search result page,
     * in an array of names of the search modules.
     */
    var categoryOrder =
        sharedPreferences.getString(searchCategoryOrderPrefKey, null)
            ?.split(CATEGORY_ORDER_LIST_SEPARATOR)
            ?: extensionManager.installedSearchModules.map { it.metadata.extensionName }
        private set

    /**
     * Returns the order the search module should appear on the search result list.
     *
     * @param searchModuleName The name of the [SearchModule]
     */
    fun orderOf(searchModuleName: String) = categoryOrder.indexOf(searchModuleName)

    fun changeSearchCategoryOrder(newOrder: List<String>) {
        sharedPreferences.edit {
            putString(
                searchCategoryOrderPrefKey,
                newOrder.joinToString(CATEGORY_ORDER_LIST_SEPARATOR)
            )
            apply()
        }
    }
}
