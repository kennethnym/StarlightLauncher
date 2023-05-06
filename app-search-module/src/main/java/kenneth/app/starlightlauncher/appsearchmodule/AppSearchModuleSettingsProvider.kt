package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kenneth.app.starlightlauncher.api.ExtensionSettingsProvider
import kenneth.app.starlightlauncher.appsearchmodule.settings.MainSettingsScreen

class AppSearchModuleSettingsProvider(context: Context) : ExtensionSettingsProvider {
    override val settingsTitle = context.getString(R.string.app_search_module_settings_title)

    override val settingsSummary = context.getString(R.string.app_search_module_settings_summary)

    override val settingsIconRes = R.drawable.app_search_module_settings_icon

    override val settingsRoutes: Map<String, @Composable () -> Unit> = mapOf(
        "root" to { MainSettingsScreen() }
    )

    /**
     * An escape hatch to allow main launcher code to access app search module preferences.
     * Needed by the launcher to access pinned apps.
     */
    fun preferences(dataStore: DataStore<Preferences>) =
        AppSearchModulePreferences.getInstance(dataStore)
}
