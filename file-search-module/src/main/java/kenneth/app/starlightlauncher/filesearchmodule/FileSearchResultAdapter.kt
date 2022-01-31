package kenneth.app.starlightlauncher.filesearchmodule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.SpotlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.filesearchmodule.databinding.FileSearchResultCardBinding

internal const val INITIAL_LIST_ITEM_COUNT = 5

internal class FileSearchResultAdapter(
    private val prefs: FileSearchModulePreferences,
    private val launcher: SpotlightLauncherApi,
) : SearchResultAdapter {
    private lateinit var currentViewHolder: FileSearchResultViewHolder
    private var fileListAdapter: FileListAdapter? = null

    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding =
            FileSearchResultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply {
                    searchResultCard.blurWith(launcher.blurHandler)
                }
        return FileSearchResultViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is FileSearchResultViewHolder && searchResult is FileSearchModule.Result) {
            onBindSearchResult(holder, searchResult)
        }
    }

    private fun onBindSearchResult(
        holder: FileSearchResultViewHolder,
        searchResult: FileSearchModule.Result,
    ) {
        currentViewHolder = holder
        when {
            prefs.includedPaths.isEmpty() -> {
                with(holder.binding) {
                    isNoIncludedPathsMessageShown = true
                    isNoFilesFoundMessageShown = false
                }
                fileListAdapter = null
            }

            searchResult.files.isEmpty() -> {
                with(holder.binding) {
                    isNoIncludedPathsMessageShown = false
                    isNoFilesFoundMessageShown = true
                }
                fileListAdapter = null
            }

            else -> {
                with(holder.binding) {
                    isNoIncludedPathsMessageShown = false
                    isNoFilesFoundMessageShown = false

                    fileList.apply {
                        adapter = FileListAdapter(context, searchResult.files).also {
                            fileListAdapter = it
                        }
                        layoutManager = LinearLayoutManager(context)
                    }

                    showMoreButton.apply {
                        isVisible = searchResult.files.size > INITIAL_LIST_ITEM_COUNT
                        setOnClickListener { showMoreFiles() }
                    }
                }
            }
        }
    }

    private fun showMoreFiles() {
        fileListAdapter?.let {
            it.showMore()
            currentViewHolder.binding.showMoreButton.isVisible = it.hasMore
        }
    }
}

internal class FileSearchResultViewHolder(internal val binding: FileSearchResultCardBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}