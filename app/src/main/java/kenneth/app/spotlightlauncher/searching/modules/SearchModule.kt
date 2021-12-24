package kenneth.app.spotlightlauncher.searching.modules

import kenneth.app.spotlightlauncher.searching.SearchResult

interface SearchModule {
    fun search(keyword: String, keywordRegex: Regex): SearchResult
}
