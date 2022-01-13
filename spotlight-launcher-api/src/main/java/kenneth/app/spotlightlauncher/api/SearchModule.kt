package kenneth.app.spotlightlauncher.api

import android.content.Context
import kenneth.app.spotlightlauncher.api.view.SearchResultAdapter

/**
 * Defines a module that provides search results of a specific category
 * based on the current search term.
 */
interface SearchModule {
    val metadata: Metadata

    /**
     * The [SearchResultAdapter] that should be used to adapt search results to views.
     */
    val adapter: SearchResultAdapter

    /**
     * Initializes this [SearchModule].
     *
     * @param launcher An instance of [SpotlightLauncherApi] that allows you to interact with Spotlight Launcher.
     */
    fun initialize(launcher: SpotlightLauncherApi)

    /**
     * Called when this [SearchModule] is no longer needed.
     * Cleanup actions can be performed here.
     */
    fun cleanup()

    /**
     * Initiates search on this [SearchModule]. This [SearchModule] will provide relevant [SearchResult]
     * based on [keyword] and the [Regex] of [keyword]
     */
    fun search(keyword: String, keywordRegex: Regex): SearchResult

    data class Metadata(
        /**
         * A unique string to identify this search module.
         * For example, the full package path to this module can be used:
         * `com.my.package.MySearchModule`
         */
        val name: String,

        /**
         * A user facing name of this [SearchModule]
         */
        val displayName: String,

        /**
         * A user facing description that describes what this [SearchModule] does
         */
        val description: String,
    )
}
