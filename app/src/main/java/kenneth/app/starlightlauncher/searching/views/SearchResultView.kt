package kenneth.app.starlightlauncher.searching.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.utils.dp
import kenneth.app.starlightlauncher.api.utils.swap
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.SearchPreferenceChanged
import kenneth.app.starlightlauncher.prefs.SearchPreferenceManager
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.utils.BindingRegister
import kenneth.app.starlightlauncher.utils.activity
import kenneth.app.starlightlauncher.views.OrderedInsertionLinearLayout
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultView(context: Context, attrs: AttributeSet) :
    OrderedInsertionLinearLayout(context, attrs) {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var searchPreferenceManager: SearchPreferenceManager

    @Inject
    lateinit var appState: AppState

    override val allContainers: MutableList<Container?> =
        extensionManager.installedSearchModules
            .map { null }
            .toMutableList()

    private val viewHolders = extensionManager.installedSearchModules
        .map { null }
        .toMutableList<SearchResultAdapter.ViewHolder?>()

    init {
        fitsSystemWindows = false
        orientation = VERTICAL
        gravity = Gravity.START

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_margin_horizontal)
        val paddingTop = resources.getDimensionPixelSize(R.dimen.widget_space_between)

        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)

        with(searcher) {
            addSearchResultListener { result ->
                activity?.runOnUiThread {
                    showSearchResult(result)
                }
            }
        }

        searchPreferenceManager.addOnSearchPreferencesChangedListener {
            when (it) {
                is SearchPreferenceChanged.SearchCategoryOrderChanged -> {
                    onSearchCategoryOrderChanged(it.fromIndex, it.toIndex)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePadding(
            bottom = activity?.window?.decorView?.rootWindowInsets?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    it.getInsets(WindowInsets.Type.navigationBars()).bottom
                else
                    it.systemWindowInsetBottom
            } ?: 0
        )
    }

    fun clearSearchResults() {
        allContainers.forEach { it?.isVisible = false }
    }

    private fun showSearchResult(result: SearchResult) {
        BindingRegister.activityMainBinding.widgetsPanel.showSearchResults()
        extensionManager.lookupSearchModule(result.extensionName)?.let { searchModule ->
            val order = searchPreferenceManager.orderOf(searchModule.metadata.extensionName)

            containerAt(order)
                ?.let {
                    searchModule.adapter.onBindSearchResult(viewHolders[order]!!, result)
                    it.isVisible = true
                }
                ?: createContainerAt(order)
                    .apply {
                        layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT,
                        ).apply {
                            setMargins(0, 0, 0, 16.dp)
                        }
                        orientation = VERTICAL
                    }.also {
                        val vh = searchModule.adapter.onCreateViewHolder(it)
                        viewHolders[order] = vh
                        searchModule.adapter.onBindSearchResult(vh, result)
                        it.addView(vh.rootView)
                        it.isVisible = true
                    }
        }
    }

    private fun onSearchCategoryOrderChanged(fromIndex: Int, toIndex: Int) {
        swapContainers(fromIndex, toIndex)
        viewHolders.swap(fromIndex, toIndex)
    }
}