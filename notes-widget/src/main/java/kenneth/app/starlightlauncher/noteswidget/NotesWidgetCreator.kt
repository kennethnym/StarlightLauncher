package kenneth.app.starlightlauncher.noteswidget

import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.noteswidget.databinding.NotesWidgetBinding

class NotesWidgetCreator : WidgetCreator {
    override fun createWidget(parent: ViewGroup, launcher: StarlightLauncherApi): WidgetHolder {
        val binding = NotesWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesWidget(binding, launcher)
    }
}