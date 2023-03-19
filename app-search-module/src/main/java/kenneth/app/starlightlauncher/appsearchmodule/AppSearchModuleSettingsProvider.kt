package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kenneth.app.starlightlauncher.api.ExtensionSettingsProvider
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.appsearchmodule.settings.MainSettingsScreen

val LauncherApi = staticCompositionLocalOf<StarlightLauncherApi> {
    error("No launcher API provided.")
}

class AppSearchModuleSettingsProvider(context: Context) : ExtensionSettingsProvider {
    override val settingsTitle = context.getString(R.string.app_search_module_settings_title)

    override val settingsSummary = context.getString(R.string.app_search_module_settings_summary)

    override val settingsIconRes = R.drawable.app_search_module_settings_icon

    override val settingsRoutes: Map<String, @Composable () -> Unit> = mapOf(
        "root" to { MainSettingsScreen() }
    )
}
