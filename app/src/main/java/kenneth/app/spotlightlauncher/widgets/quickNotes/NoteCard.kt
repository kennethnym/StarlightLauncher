package kenneth.app.spotlightlauncher.widgets.quickNotes

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import kenneth.app.spotlightlauncher.databinding.NoteCardBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.TimeAgo
import kotlin.time.ExperimentalTime

class NoteCard(
    context: Context,
    private val adapter: NoteCardListAdapter,
    override val binding: NoteCardBinding,
    private val timeAgo: TimeAgo,
    private val notesPreferenceManager: NotesPreferenceManager,
) :
    RecyclerViewDataAdapter.ViewHolder<Note>(binding) {
    private val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private lateinit var note: Note

    @ExperimentalTime
    override fun bindWith(data: Note) {
        note = data
        with(binding) {
            noteTimestamp.text = timeAgo.prettify(note.createdOn)
            noteContent.text = note.content

            noteCardEditButton.setOnClickListener { switchToEditMode() }
            deleteNoteButton.setOnClickListener { deleteNote() }
        }
    }

    @ExperimentalTime
    private fun switchToEditMode() {
        with(binding) {
            noteContent.isVisible = false
            noteCardActionBarNormal.isVisible = false
            noteCardActionBarEditing.isVisible = true

            noteContentEditText.apply {
                isVisible = true
                setText(note.content)
            }.also {
                it.requestFocus()
                inputMethodManager.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }

            noteCardCancelEdit.setOnClickListener { turnOffEditMode() }
            noteCardSaveEdit.setOnClickListener { saveEdit() }
        }
    }

    private fun turnOffEditMode() {
        with(binding) {
            noteContent.isVisible = true
            noteContentEditText.isVisible = false
            noteCardActionBarNormal.isVisible = true
            noteCardActionBarEditing.isVisible = false

            inputMethodManager.hideSoftInputFromWindow(noteContentEditText.windowToken, 0)
        }
    }

    @ExperimentalTime
    private fun saveEdit() {
        val newNoteContent = binding.noteContentEditText.text.toString()
        val newNote = note.copy(
            content = newNoteContent,
        )

        notesPreferenceManager.editNote(newNote)
        turnOffEditMode()
        bindWith(newNote)
    }

    private fun deleteNote() {
        notesPreferenceManager.deleteNote(note)
        adapter.notifyItemRemoved(adapterPosition)
    }
}