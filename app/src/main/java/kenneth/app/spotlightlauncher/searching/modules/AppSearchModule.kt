package kenneth.app.spotlightlauncher.searching.modules

import kenneth.app.spotlightlauncher.searching.AppManager
import kenneth.app.spotlightlauncher.searching.SearchResult

class AppSearchModule(private val appManager: AppManager) : SearchModule {
    override fun search(keyword: String, keywordRegex: Regex): SearchResult =
        SearchResult.Apps(
            keyword,
            apps = appManager.searchApps(keywordRegex)
        )
}
