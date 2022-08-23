package kenneth.app.starlightlauncher.searching.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.util.dp
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceChanged
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceManager
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.api.util.activity
import kenneth.app.starlightlauncher.views.OrderedInsertionLinearLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
internal class SearchResultView(context: Context, attrs: AttributeSet) :
    OrderedInsertionLinearLayout(context, attrs) {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var searchPreferenceManager: SearchPreferenceManager

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var launcherEventChannel: LauncherEventChannel

    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    lateinit var bindingRegister: BindingRegister

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

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_list_padding)
        val paddingTop = resources.getDimensionPixelSize(R.dimen.widget_list_space_between) * 2

        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)

        with(searcher) {
            addSearchResultListener { result ->
                showSearchResult(result)
            }
        }

        CoroutineScope(mainDispatcher).launch {
            launcherEventChannel.subscribe(::onLauncherEvent)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePadding(
            bottom = activity?.window?.decorView?.rootWindowInsets?.let {
                WindowInsetsCompat.toWindowInsetsCompat(it)
                    .getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            } ?: 0
        )
    }

    fun clearSearchResults() {
        allContainers.forEach { it?.isVisible = false }
    }

    private fun onLauncherEvent(event: LauncherEvent) {
        when (event) {
            is SearchPreferenceChanged.SearchCategoryOrderChanged -> {
                onSearchCategoryOrderChanged(event.fromIndex, event.toIndex)
            }
        }
    }

    private fun showSearchResult(result: SearchResult) {
        if (result is SearchResult.None) {
            val order = searchPreferenceManager.orderOf(result.extensionName)
            allContainers[order]?.isVisible = false
        } else {
            bindingRegister.mainScreenBinding.widgetsPanel.showSearchResults()
            extensionManager.lookupSearchModule(result.extensionName)?.let { searchModule ->
                val order = searchPreferenceManager.orderOf(searchModule.metadata.extensionName)

                if (order < 0) {
                    // this search module cannot be found for some reason
                    // we ignore it to avoid crash.
                    //
                    // if this happens the launcher is probably in a weird/corrupted state.
                    return
                }

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
    }

    private fun onSearchCategoryOrderChanged(fromIndex: Int, toIndex: Int) {
        swapContainers(fromIndex, toIndex)
        viewHolders.swap(fromIndex, toIndex)
    }
}