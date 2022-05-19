package kenneth.app.starlightlauncher.wificontrolmodule

import android.content.Context
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.wificontrolmodule"

class WifiControlModule(context: Context) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = EXTENSION_NAME,
        displayName = context.getString(R.string.wifi_control_module_display_name),
        description = context.getString(R.string.permissions_required_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        val mainContext = launcher.context

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