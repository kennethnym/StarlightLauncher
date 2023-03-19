package kenneth.app.starlightlauncher.noteswidget

import android.content.Context
import androidx.compose.runtime.Composable
import kenneth.app.starlightlauncher.api.ExtensionSettingsProvider
import kenneth.app.starlightlauncher.noteswidget.settings.MainSettingsScreen

class NotesWidgetSettingsProvider(context: Context) : ExtensionSettingsProvider {
    override val settingsTitle = context.getString(R.string.notes_widget_settings_title)

    override val settingsSummary = context.getString(R.string.notes_widget_settings_description)

    override val settingsIconRes = R.drawable.ic_notes

    override val settingsRoutes: Map<String, @Composable () -> Unit> = mapOf(
        "root" to { MainSettingsScreen() }
    )
}
