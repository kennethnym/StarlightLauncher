package kenneth.app.starlightlauncher.websearchmodule

import android.content.Context
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.websearchmodule"

class WebSearch(context: Context) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = EXTENSION_NAME,
        displayName = context.getString(R.string.app_name),
        description = context.getString(R.string.web_search_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        adapter = WebSearchAdapter(launcher.context, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        Result(keyword)

    internal class Result(val search: String) : SearchResult(search, EXTENSION_NAME)
}