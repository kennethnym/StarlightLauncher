package kenneth.app.starlightlauncher.api

/**
 * Defines a search result produced by a [SearchModule] given a [query].
 */
abstract class SearchResult(
    /**
     * The search query that produced this [SearchResult]
     */
    val query: String,

    /**
     * The name of the extension containing the search module
     * that produced this [SearchResult]
     */
    val extensionName: String,
)
