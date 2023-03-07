package kenneth.app.starlightlauncher.filesearchmodule

import android.content.Context
import androidx.compose.runtime.Composable
import kenneth.app.starlightlauncher.api.ExtensionSettingsProvider
import kenneth.app.starlightlauncher.filesearchmodule.settings.MainSettingsScreen

class FileSearchModuleSettingsProvider(context: Context) : ExtensionSettingsProvider {
    override val settingsTitle =
        context.getString(R.string.file_search_module_search_module_settings_title)

    override val settingsSummary =
        context.getString(R.string.file_search_module_search_module_settings_description)

    override val settingsIconRes = R.drawable.ic_file_search_alt

    override val settingsRoutes: Map<String, @Composable () -> Unit> = mapOf(
        "root" to { MainSettingsScreen() }
    )
}