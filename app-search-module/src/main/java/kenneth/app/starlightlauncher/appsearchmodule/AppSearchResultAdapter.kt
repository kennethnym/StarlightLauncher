package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppSearchResultCardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Defines how many apps are shown when the app grid is displayed initially.
 */
private const val INITIAL_ITEM_COUNT = 10

class AppSearchResultAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi,
) :
    SearchResultAdapter {
    private lateinit var appList: AppList
    private lateinit var currentViewHolder: AppSearchResultViewHolder

    private var appGridAdapter: AppGridAdapter? = null
    private val prefs = AppSearchModulePreferences.getInstance(launcher)

    init {
        launcher.coroutineScope.run {
            launch { launcher.addLauncherEventListener(::onLauncherEvent) }
            // listen to show app names visibility changes
            launch {
                launcher.dataStore.data
                    .map { preferences ->
                        preferences[PREF_KEY_SHOW_APP_NAMES]
                            ?: context.resources.getBoolean(R.bool.def_pref_show_app_names)
                    }
                    .collect { shouldShowAppNames ->
                        if (shouldShowAppNames) {
                            appGridAdapter?.showAppLabels()
                        } else {
                            appGridAdapter?.hideAppLabels()
                        }
                    }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): AppSearchResultViewHolder {
        val binding =
            AppSearchResultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply {
                    appSearchResultCard.blurWith(launcher.blurHandler)
                }
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

    private fun onLauncherEvent(event: LauncherEvent) {
        when (event) {
            is LauncherEvent.IconPackChanged -> {
                appGridAdapter?.refresh()
            }
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

                val appGridAdapter =
                    AppGridAdapter(
                        context,
                        appList,
                        launcher,
                        shouldShowAppNames = runBlocking { prefs.shouldShowAppNames.first() }
                    )
                        .also { appGridAdapter = it }

                appGrid.apply {
                    layoutManager = GridLayoutManager(context, 5)
                    adapter = appGridAdapter
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
            it.showMore()
            currentViewHolder.binding
                .showMoreButton.isVisible = it.hasMore()
        }
    }
}

class AppSearchResultViewHolder(val binding: AppSearchResultCardBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}