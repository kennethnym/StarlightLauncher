package kenneth.app.starlightlauncher.urlopener

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.urlopener.databinding.OpenUrlBinding

class UrlOpenerAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi
) :
    SearchResultAdapter {
    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding = OpenUrlBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UrlOpenerViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is UrlOpenerViewHolder && searchResult is UrlOpener.IsValidUrl) {
            with(holder.binding) {
                urlOpenerBg.blurWith(launcher.blurHandler)
                url = searchResult.url
                urlButton.setOnClickListener { open(searchResult.url) }
            }
        }
    }

    /**
     * Opens the given url in the default browser.
     */
    private fun open(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fixUrl(url)))
        context.startActivity(browserIntent)
    }

    /**
     * Fixes missing https issues in the given url
     */
    private fun fixUrl(url: String): String =
        if (url.startsWith("www."))
            "https://$url"
        else url
}

class UrlOpenerViewHolder(internal val binding: OpenUrlBinding) : SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}

