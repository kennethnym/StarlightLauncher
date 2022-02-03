package kenneth.app.starlightlauncher.noteswidget

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.noteswidget.databinding.NotesWidgetBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import kenneth.app.starlightlauncher.noteswidget.view.QuickNoteListAdapter

internal class NotesWidget(
    private val binding: NotesWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder {
    private val context = binding.root.context

    private val prefs = NotesWidgetPreferences.getInstance(context)

    override val rootView: View = binding.root

    init {
        with(binding) {
            quickNoteList.apply {
                adapter = QuickNoteListAdapter(context)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }

            addNoteButton.setOnClickListener { addNote() }

            addNotesEditText.addTextChangedListener {
                addNoteButton.isInvisible = it?.isBlank() == true
            }

            notesWidget.blurWith(launcher.blurHandler)
        }
    }

    private fun addNote() {
        binding.addNotesEditText.text.run {
            if (this.isNotBlank()) {
                val newNote = Note(content = toString())
                prefs.addNote(newNote)
            }
        }
    }
}