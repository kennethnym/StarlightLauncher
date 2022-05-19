package kenneth.app.starlightlauncher.api

import android.content.Context
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

/**
 * Defines a module that provides search results of a specific category
 * based on the current search term.
 *
 * @param context The application context of this search module.
 *                This is different from the application context of the launcher itself.
 *                This context can be used to query strings/resources declared in the same package as this search module.
 *                To access the shared preferences of the launcher or any service related to the launcher,
 *                please use the context object from [StarlightLauncherApi].
 */
abstract class SearchModule(protected val context: Context) {
    abstract val metadata: Metadata

    /**
     * The [SearchResultAdapter] that should be used to adapt search results to views.
     */
    abstract val adapter: SearchResultAdapter

    /**
     * Initializes this [SearchModule].
     *
     * @param launcher An instance of [StarlightLauncherApi] that allows you to interact with Spotlight Launcher.
     */
    abstract fun initialize(launcher: StarlightLauncherApi)

    /**
     * Called when this [SearchModule] is no longer needed.
     * Cleanup actions can be performed here.
     */
    abstract fun cleanup()

    /**
     * Initiates search on this [SearchModule]. This [SearchModule] will provide relevant [SearchResult]
     * based on [keyword] and the [Regex] of [keyword]
     */
    abstract suspend fun search(keyword: String, keywordRegex: Regex): SearchResult

    data class Metadata(
        /**
         * A unique string to identify the extension containing the search module.
         * The package name of the extension should be used, e.g. `com.my.extension`.
         */
        val extensionName: String,

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
