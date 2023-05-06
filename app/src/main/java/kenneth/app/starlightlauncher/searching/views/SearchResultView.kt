package kenneth.app.starlightlauncher.searching.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.util.activity
import kenneth.app.starlightlauncher.api.util.dp
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.views.OrderedInsertionLinearLayout

@AndroidEntryPoint
internal class SearchResultView(context: Context, attrs: AttributeSet) :
    OrderedInsertionLinearLayout(context, attrs) {
    private var viewHolders = mutableListOf<SearchResultAdapter.ViewHolder?>()

    var searchModules: Collection<SearchModule> = emptyList()
        set(value) {
            field = value
            allContainers = MutableList(value.size) { null }
            viewHolders = MutableList(value.size) { null }
        }

    var searchModuleOrder: List<String> = emptyList()

    init {
        fitsSystemWindows = false
        orientation = VERTICAL
        gravity = Gravity.START

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_list_padding)
        val paddingTop = resources.getDimensionPixelSize(R.dimen.widget_list_space_between) * 2

        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)
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

    /**
     * Displays the given search result.
     *
     * @param result The [SearchResult] to be displayed
     * @param searchModule The [SearchModule] that produced the search result.
     */
    fun showSearchResult(result: SearchResult, searchModule: SearchModule) {
        val order = searchModuleOrder.indexOf(result.extensionName)

        if (result is SearchResult.None) {
            allContainers[order]?.isVisible = false
        } else {
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

    fun swapPosition(fromPosition: Int, toPosition: Int) {
        swapContainers(fromPosition, toPosition)
        viewHolders.swap(fromPosition, toPosition)
    }
}