package kenneth.app.starlightlauncher.api

import androidx.compose.runtime.Composable

interface ExtensionSettingsProvider {
    val settingsTitle: String
    val settingsSummary: String
    val settingsIconRes: Int
    val settingsRoutes: Map<String, @Composable () -> Unit>
}
