package kenneth.app.spotlightlauncher.api.view

import android.view.View
import android.view.ViewGroup
import kenneth.app.spotlightlauncher.api.SearchResult

/**
 * Responsible for
 */
interface SearchResultAdapter {
    /**
     * Called when a [ViewHolder] is needed to display search results.
     *
     * @param parent The parent of the [View] held by the [ViewHolder] returned by this method.
     * @return A [ViewHolder] that holds the [View] that will be responsible for display search results.
     */
    fun onCreateViewHolder(parent: ViewGroup): ViewHolder

    /**
     * Called when a search result needs to be bound to the given [ViewHolder].
     *
     * @param holder The [ViewHolder] that should display the [SearchResult].
     * @param searchResult The [SearchResult] that should be displayed.
     */
    fun onBindSearchResult(holder: ViewHolder, searchResult: SearchResult)

    /**
     * Holds the [View] that will display any given search results.
     */
    interface ViewHolder {
        val rootView: View
    }
}