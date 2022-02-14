package kenneth.app.starlightlauncher.prefs.searchlayout

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.utils.swap
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.SearchPreferenceManager
import kenneth.app.starlightlauncher.prefs.views.ReorderablePreference
import javax.inject.Inject

@AndroidEntryPoint
class SearchLayoutSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var searchPreferenceManager: SearchPreferenceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    private val searchModuleOrder = mutableListOf<String>()

    private var searchCategoryOrderPref: ReorderablePreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.search_layout_preferences, rootKey)

        searchModuleOrder += searchPreferenceManager.categoryOrder.filter { extName ->
            extensionManager.hasSearchModule(
                extName
            )
        }

        findPreference<ReorderablePreference>(getString(R.string.search_category_order_pref_key))
            ?.apply {
                items = searchModuleOrder.mapNotNull { extName ->
                    extensionManager.lookupSearchModule(extName)?.metadata?.let {
                        ReorderablePreference.Item(
                            value = extName,
                            title = it.displayName,
                            summary = it.description,
                        )
                    }
                }
                setOnPreferenceOrderChanged(::onSearchCategoryOrderChanged)
            }
            .also { searchCategoryOrderPref = it }
    }

    private fun onSearchCategoryOrderChanged(fromIndex: Int, toIndex: Int) {
        searchModuleOrder.swap(fromIndex, toIndex)
        searchPreferenceManager.changeSearchCategoryOrder(fromIndex, toIndex, searchModuleOrder)
    }
}