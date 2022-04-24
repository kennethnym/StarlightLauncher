package kenneth.app.starlightlauncher.mathsearchmodule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.mathsearchmodule.databinding.MathSearchResultCardBinding

class MathSearchResultAdapter(
    private val launcher: StarlightLauncherApi,
) : SearchResultAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding =
            MathSearchResultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MathSearchResultViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is MathSearchResultViewHolder && searchResult is MathSearchModule.Result) {
            onBindSearchResult(holder, searchResult)
        }
    }

    private fun onBindSearchResult(
        holder: MathSearchResultViewHolder,
        searchResult: MathSearchModule.Result
    ) {
        holder.binding.apply {
            expression = searchResult.query
            value = searchResult.value
        }

        holder.binding.mathSearchResultCard.blurWith(launcher.blurHandler)
    }
}

class MathSearchResultViewHolder(internal val binding: MathSearchResultCardBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView: View = binding.root
}