package kenneth.app.spotlightlauncher.api

/**
 * Defines a search result produced by a [SearchModule] given a [query].
 */
abstract class SearchResult(
    /**
     * The search query that produced this [SearchResult]
     */
    val query: String,

    /**
     * The name of the [SearchModule] that produced this [SearchResult]
     */
    val searchModuleName: String,
)
