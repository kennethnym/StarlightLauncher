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
import kenneth.app.starlightlauncher.noteswidget.view.AllNotes
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
            showAllNotesButton.setOnClickListener { showAllNotes() }
            addNotesEditText.addTextChangedListener {
                addNoteButton.isInvisible = it?.isBlank() == true
            }

            notesWidget.blurWith(launcher.blurHandler)
        }
    }

    private fun addNote() {
        binding.addNotesEditText.run {
            if (text.isNotBlank()) {
                val newNote = Note(content = text.toString())
                prefs.addNote(newNote)
                // clear text field after a note is added
                text.clear()
            }
        }
    }

    private fun showAllNotes() {
        launcher.showOverlay(rootView, ::AllNotes)
    }
}