package kenneth.app.starlightlauncher.prefs.searching

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject
import javax.inject.Singleton

private const val CATEGORY_ORDER_LIST_SEPARATOR = ";"

sealed class SearchPreferenceChanged {
    data class SearchCategoryOrderChanged(
        val fromIndex: Int,
        val toIndex: Int
    ) :
        SearchPreferenceChanged()
}

typealias SearchPreferenceChangedListener = (event: SearchPreferenceChanged) -> Unit

@Singleton
internal class SearchPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val extensionManager: ExtensionManager,
) : ObservablePreferences<SearchPreferenceManager>(context) {
    val keys = SearchPreferencesPrefKeys(context)

    private val _enabledSearchModules =
        sharedPreferences.getStringSet(keys.enabledSearchModules, null)
            ?.toMutableSet()
            ?: mutableSetOf<String>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.searchModule != null) add(ext.name)
                }
            }

    /**
     * Defines the order the search modules should appear on the search result page,
     * in an array of names of the search modules.
     */
    var categoryOrder =
        sharedPreferences.getString(keys.searchCategoryOrder, null)
            ?.split(CATEGORY_ORDER_LIST_SEPARATOR)
            ?: extensionManager.installedSearchModules.map { it.metadata.extensionName }
        private set

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    /**
     * Returns the order the search module should appear on the search result list.
     *
     * @param searchModuleName The name of the [SearchModule]
     */
    fun orderOf(searchModuleName: String) = categoryOrder.indexOf(searchModuleName)

    fun changeSearchCategoryOrder(fromIndex: Int, toIndex: Int, newOrder: List<String>) {
        categoryOrder = newOrder
        sharedPreferences.edit(commit = true) {
            putString(
                keys.searchCategoryOrder,
                newOrder.joinToString(CATEGORY_ORDER_LIST_SEPARATOR)
            )
        }
        setChanged()
        notifyObservers(SearchPreferenceChanged.SearchCategoryOrderChanged(fromIndex, toIndex))
    }

    fun addOnSearchPreferencesChangedListener(listener: SearchPreferenceChangedListener) {
        addObserver { o, arg ->
            if (arg is SearchPreferenceChanged) {
                listener(arg)
            }
        }
    }
}

class SearchPreferencesPrefKeys(context: Context) {
    val searchCategoryOrder by lazy { context.getString(R.string.search_category_order_pref_key) }

    val enabledSearchModules by lazy { context.getString(R.string.search_enabled_modules_pref_key) }
}
