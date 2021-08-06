package kenneth.app.spotlightlauncher.searching

import android.content.pm.ResolveInfo
import androidx.documentfile.provider.DocumentFile

/**
 * Result of a search query produced by [Searcher].
 * Contains various subclasses that represent different types of search results.
 *
 * @property query The query of the search
 */
sealed class SearchResult(val query: String) {
    /**
     * List of apps that match the search query.
     */
    class Apps(query: String, val apps: List<ResolveInfo>) : SearchResult(query)

    /**
     * List of files that match the search query.
     */
    class Files(query: String, val files: List<DocumentFile>?) : SearchResult(query)

    class None(query: String) : SearchResult(query)

    /**
     * Suggested search result produced by [SmartSearcher].
     * Contains various subclasses that represent different types of suggestions.
     */
    sealed class Suggested(query: String) : SearchResult(query) {
        /**
         * [query] is a valid math expression, and [result] contains the result of the expression.
         *
         * @property result The result of the math expression
         */
        class Math(query: String, val result: Float) : Suggested(query)

        class App(query: String, val suggestedApp: ResolveInfo) : Suggested(query)

        class Wifi(query: String) : Suggested(query)

        class Bluetooth(query: String) : Suggested(query)

        class Url(query: String) : Suggested(query)

        class None(query: String) : Suggested(query)
    }
}