package kenneth.app.starlightlauncher.widgets.quicknotes

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import kenneth.app.starlightlauncher.spotlightlauncher.databinding.NoteCardBinding
import kenneth.app.starlightlauncher.models.Note
import kenneth.app.starlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.starlightlauncher.utils.BindingRegister
import kenneth.app.starlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.starlightlauncher.utils.TimeAgo
import kotlin.time.ExperimentalTime

@ExperimentalTime
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

    override fun bindWith(data: Note) {
        with(binding) {
            note = data
            timeAgo = this@NoteCard.timeAgo

            noteCardEditButton.setOnClickListener { switchToEditMode() }
            deleteNoteButton.setOnClickListener { deleteNote() }
        }

        if (adapter.hasNewItem) {
            switchToEditMode()
            adapter.hasNewItem = false
        }
    }

    private fun switchToEditMode() {
        with(binding) {
            noteContent.isVisible = false
            noteCardActionBarNormal.isVisible = false
            noteCardActionBarEditing.isVisible = true

            noteContentEditText.apply {
                isVisible = true
                setText(note!!.content)
            }.also {
                it.requestFocus()
                inputMethodManager.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }

            noteCardCancelEdit.setOnClickListener { turnOffEditMode() }
            noteCardSaveEdit.setOnClickListener { saveEdit() }
        }
        BindingRegister.activityMainBinding.widgetsPanel.avoidView(binding.root)
    }

    private fun turnOffEditMode() {
        with(binding) {
            noteContent.isVisible = true
            noteContentEditText.isVisible = false
            noteCardActionBarNormal.isVisible = true
            noteCardActionBarEditing.isVisible = false

            inputMethodManager.hideSoftInputFromWindow(noteContentEditText.windowToken, 0)
        }
        BindingRegister.activityMainBinding.widgetsPanel.stopAvoidingView()
    }

    private fun saveEdit() {
        val newNoteContent = binding.noteContentEditText.text.toString()
        val newNote = binding.note!!.copy(
            content = newNoteContent,
        )

        notesPreferenceManager.editNote(newNote)
        turnOffEditMode()
        bindWith(newNote)
    }

    private fun deleteNote() {
        notesPreferenceManager.deleteNote(binding.note!!)
        adapter.notifyItemRemoved(adapterPosition)
    }
}