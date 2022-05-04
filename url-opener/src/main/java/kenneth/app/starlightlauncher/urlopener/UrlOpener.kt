package kenneth.app.starlightlauncher.urlopener

import android.util.Patterns
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.urlopener"

class UrlOpener : SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        metadata = SearchModule.Metadata(
            extensionName = EXTENSION_NAME,
            displayName = launcher.context.getString(R.string.url_opener_display_name),
            description = launcher.context.getString(R.string.url_opener_description),
        )

        adapter = UrlOpenerAdapter(launcher.context, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        if (Patterns.WEB_URL.matcher(keyword).matches())
            IsValidUrl(keyword)
        else
            SearchResult.None(keyword, EXTENSION_NAME)

    internal class IsValidUrl(val url: String) : SearchResult(url, EXTENSION_NAME)
}