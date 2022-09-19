package kenneth.app.starlightlauncher.noteswidget

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.noteswidget.databinding.NotesWidgetBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import kenneth.app.starlightlauncher.noteswidget.view.AllNotesFragment
import kenneth.app.starlightlauncher.noteswidget.view.NoteListDiffCallback
import kenneth.app.starlightlauncher.noteswidget.view.QuickNoteListAdapter
import kenneth.app.starlightlauncher.noteswidget.view.QuickNoteListAdapterCallback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * The max number of notes that is shown in the widget.
 * Users need to click on All Notes button to view all the notes they have made.
 */
internal const val MAX_NOTES_SHOWN_IN_WIDGET = 5

internal class NotesWidget(
    private val binding: NotesWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder, QuickNoteListAdapterCallback {
    private val prefs = NotesWidgetPreferences.getInstance(launcher)

    private val quickNoteListAdapter: QuickNoteListAdapter

    override val rootView: View = binding.root

    init {
        with(binding) {
            quickNoteList.apply {
                adapter = QuickNoteListAdapter(this@NotesWidget).also {
                    quickNoteListAdapter = it
                }
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

        launcher.coroutineScope.launch {
            subscribeToNoteList()
        }
    }

    override fun onNoteDeleted(deletedNote: Note) {
        launcher.coroutineScope.launch {
            prefs.deleteNote(deletedNote)
        }
    }

    private suspend fun subscribeToNoteList() {
        prefs.notes.take(MAX_NOTES_SHOWN_IN_WIDGET).collect { notes ->
            val oldNotes = quickNoteListAdapter.notes
            quickNoteListAdapter.notes = notes
            DiffUtil.calculateDiff(NoteListDiffCallback(oldNotes, notes))
                .dispatchUpdatesTo(quickNoteListAdapter)
        }
    }

    private fun addNote() {
        launcher.coroutineScope.launch {
            val text = binding.addNotesEditText.text
            if (text.isNotBlank()) {
                val newNote = Note(content = text.toString())
                prefs.addNote(newNote)
                // clear text field after a note is added
                text.clear()
            }
        }
    }

    private fun showAllNotes() {
        launcher.showOverlay(AllNotesFragment(launcher))
    }
}