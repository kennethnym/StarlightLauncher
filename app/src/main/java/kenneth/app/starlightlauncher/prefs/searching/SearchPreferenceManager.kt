package kenneth.app.starlightlauncher.prefs.searching

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.extension.DEFAULT_EXTENSIONS
import kenneth.app.starlightlauncher.extension.DEFAULT_EXTENSION_NAMES
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
) : ObservablePreferences<SearchPreferenceManager>(context) {
    val keys = SearchPreferencesPrefKeys(context)

    private val _enabledSearchModules =
        sharedPreferences.getStringSet(keys.enabledSearchModules, null)
            ?.toMutableSet()
            ?: mutableSetOf<String>().apply {
                DEFAULT_EXTENSIONS.forEach { (_, ext) ->
                    if (ext.searchModule != null) add(ext.name)
                }
            }

    private var _categoryOrder =
        sharedPreferences.getString(keys.searchCategoryOrder, null)
            ?.split(CATEGORY_ORDER_LIST_SEPARATOR)
            ?.toMutableList()
            ?: DEFAULT_EXTENSION_NAMES.toMutableList()

    /**
     * Defines the order the search modules should appear on the search result page,
     * in an array of names of the search modules.
     */
    val categoryOrder
        get() = _categoryOrder as List<String>

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    /**
     * Adds the given search module name to the order list.
     */
    fun addNewSearchModule(searchModuleName: String) {
        _categoryOrder += searchModuleName
        saveOrderList()
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

    private fun saveOrderList() {
        sharedPreferences.edit(commit = true) {
            putString(
                keys.searchCategoryOrder,
                _categoryOrder.joinToString(CATEGORY_ORDER_LIST_SEPARATOR)
            )
        }
    }
}

class SearchPreferencesPrefKeys(context: Context) {
    val searchCategoryOrder by lazy { context.getString(R.string.pref_key_search_category_order) }

    val enabledSearchModules by lazy { context.getString(R.string.search_enabled_modules_pref_key) }
}
