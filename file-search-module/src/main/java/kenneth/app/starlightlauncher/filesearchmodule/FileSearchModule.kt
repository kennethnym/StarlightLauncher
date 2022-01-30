package kenneth.app.starlightlauncher.filesearchmodule

import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.SpotlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

class FileSearchModule : SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override val adapter: SearchResultAdapter
        get() = TODO("Not yet implemented")

    override fun initialize(launcher: SpotlightLauncherApi) {

    }

    override fun cleanup() {
        TODO("Not yet implemented")
    }

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult {
        TODO("Not yet implemented")
    }
}