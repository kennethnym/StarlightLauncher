package kenneth.app.spotlightlauncher.appsearchmodule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kenneth.app.spotlightlauncher.api.SearchResult
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.view.SearchResultAdapter
import kenneth.app.spotlightlauncher.appsearchmodule.databinding.AppSearchResultCardBinding
import kotlin.math.min

/**
 * Defines how many apps are shown when the app grid is displayed initially.
 */
private const val INITIAL_ITEM_COUNT = 10

class AppSearchResultAdapter(
    private val module: AppSearchModule,
    private val launcher: SpotlightLauncherApi
) :
    SearchResultAdapter {
    private lateinit var appList: AppList
    private lateinit var currentViewHolder: AppSearchResultViewHolder
    private var appGridAdapter: AppGridAdapter? = null

    override fun onCreateViewHolder(parent: ViewGroup): AppSearchResultViewHolder {
        val binding =
            AppSearchResultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppSearchResultViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is AppSearchResultViewHolder && searchResult is AppSearchModule.Result) {
            onBindSearchResult(holder, searchResult)
        }
    }

    private fun onBindSearchResult(
        holder: AppSearchResultViewHolder,
        searchResult: AppSearchModule.Result,
    ) {
        currentViewHolder = holder

        with(holder.binding) {
            if (searchResult.apps.isEmpty()) {
                appGrid.isVisible = false
                showMoreButton.isVisible = false
                noResultLabel.isVisible = true
            } else {
                appList = searchResult.apps

                val initialAppGridItems =
                    if (appList.size > INITIAL_ITEM_COUNT)
                        appList
                            .subList(0, INITIAL_ITEM_COUNT)
                            .toMutableList()
                    else
                        appList.toMutableList()

                val appGridAdapter =
                    this@AppSearchResultAdapter.appGridAdapter?.apply {
                        apps.clear()
                        apps.addAll(initialAppGridItems)
                    }
                        ?: AppGridAdapter(module, initialAppGridItems, launcher).also {
                            appGridAdapter = it
                        }

                appGrid.apply {
                    adapter = appGridAdapter
                    layoutManager = GridLayoutManager(context, 5)
                }

                showMoreButton.apply {
                    isVisible = appList.size > INITIAL_ITEM_COUNT
                    setOnClickListener { showMoreApps() }
                }

                appGrid.isVisible = true
                noResultLabel.isVisible = false
            }
        }
    }

    private fun showMoreApps() {
        appGridAdapter?.let {
            // the total number of apps that can be displayed
            val totalItemCount = appList.size
            // the current number of items in the grid
            val currentItemCount = it.itemCount
            // the number of new apps to be added to the grid
            val addedItemsCount = min(INITIAL_ITEM_COUNT, totalItemCount - currentItemCount)
            // the total number of items after the items are added
            val newItemCount = currentItemCount + addedItemsCount

            it.apps.addAll(
                appList.subList(
                    currentItemCount,
                    min(totalItemCount, newItemCount)
                )
            )

            currentViewHolder.binding
                .showMoreButton.isVisible = newItemCount < totalItemCount

            it.notifyItemRangeInserted(currentItemCount, addedItemsCount)
        }
    }
}

class AppSearchResultViewHolder(val binding: AppSearchResultCardBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}