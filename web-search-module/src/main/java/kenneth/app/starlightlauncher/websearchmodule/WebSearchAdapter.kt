package kenneth.app.starlightlauncher.websearchmodule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.LayoutInflater
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.websearchmodule.databinding.WebSearchBinding

class WebSearchAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi
) :
    SearchResultAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding = WebSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebSearchBindingHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is WebSearchBindingHolder && searchResult is WebSearch.Result) {
            with (holder.binding) {
                webSearchBg.blurWith(launcher.blurHandler)
                search = searchResult.search
                searchButton.setOnClickListener { open(searchResult.search) }
            }
        }
    }

    private fun open(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.duckduckgo.com/?q=$url"))
        context.startActivity(browserIntent)
    }
}

class WebSearchBindingHolder(internal val binding: WebSearchBinding) : SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}
