package kenneth.app.starlightlauncher.wificontrolmodule

import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.wificontrolmodule"

class WifiControlModule : SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        val mainContext = launcher.context

        metadata = SearchModule.Metadata(
            extensionName = EXTENSION_NAME,
            displayName = mainContext.getString(R.string.wifi_control_module_display_name),
            description = mainContext.getString(R.string.permissions_required_description),
        )
        adapter = WifiControlAdapter(mainContext, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        if (keyword.lowercase() == "wifi")
            ShowControl(keyword)
        else
            SearchResult.None(keyword, EXTENSION_NAME)

    /**
     * This [SearchResult] indicates that the query triggers the wifi control.
     */
    internal class ShowControl(query: String) : SearchResult(query, EXTENSION_NAME)
}