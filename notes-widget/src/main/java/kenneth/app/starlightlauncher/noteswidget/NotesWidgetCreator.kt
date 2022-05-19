package kenneth.app.starlightlauncher.noteswidget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.noteswidget.databinding.NotesWidgetBinding

class NotesWidgetCreator(context: Context) : WidgetCreator(context) {
    override val metadata = Metadata(
        extensionName = "kenneth.app.starlightlauncher.noteswidget",
        displayName = context.getString(R.string.notes_widget_display_name),
        description = context.getString(R.string.notes_widget_description),
    )

    override fun createWidget(parent: ViewGroup, launcher: StarlightLauncherApi): WidgetHolder {
        val binding = NotesWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesWidget(binding, launcher)
    }
}