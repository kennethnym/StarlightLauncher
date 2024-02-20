package kenneth.app.starlightlauncher.noteswidget.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.databinding.NoteCardBinding

interface NoteListChangedCallback {
    /**
     * User deleted the given [Note].
     */
    fun onNoteDeleted(deletedNote: Note)

    /**
     * User edited the given [Note].
     */
    fun onNoteEdited(editedNote: Note)
}

internal class AllNoteCardListAdapter(
    context: Context,
    private val callback: NoteListChangedCallback
) :
    RecyclerView.Adapter<NoteCard>() {
    private val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private var newNoteCreated = false

    var notes = emptyList<Note>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCard {
        val binding = NoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteCard(binding)
    }

    override fun onBindViewHolder(holder: NoteCard, position: Int) {
        val currentNote = notes[position]

        // whether the note at this position is a newly created note.
        // newly created note is always appended to the end of the list
        // and newNoteCreated is set to true when the user presses the add note button
        //
        // if this note is newly created, it should immediately go into edit mode
        // so that the user can start writing to it.
        val isNewlyCreatedNote = position == notes.size - 1 && newNoteCreated

        with(holder.binding) {
            note = currentNote
            inEditMode = isNewlyCreatedNote
            areControlsEnabled = true

            noteContentEditText.run {
                requestFocus()
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }

            noteCardEditButton.setOnClickListener { turnOnEditMode(holder) }
            deleteNoteButton.setOnClickListener { deleteNote(holder, currentNote) }
            noteCardCancelEdit.setOnClickListener {
                if (isNewlyCreatedNote) {
                    newNoteCreated = false
                    callback.onNoteDeleted(currentNote)
                } else {
                    turnOffEditMode(holder)
                }
            }
            noteCardSaveEdit.setOnClickListener { saveEdit(holder, currentNote) }
        }
    }

    override fun getItemCount(): Int = notes.size

    internal fun addNote() {
        newNoteCreated = true
    }

    private fun deleteNote(holder: NoteCard, note: Note) {
        holder.binding.areControlsEnabled = false
        callback.onNoteDeleted(note)
    }

    private fun turnOnEditMode(holder: NoteCard) {
        with(holder.binding) {
            inEditMode = true
            if (noteContentEditText.requestFocus()) {
                inputMethodManager
                    .showSoftInput(noteContentEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun turnOffEditMode(holder: NoteCard) {
        with(holder.binding) {
            inEditMode = false
            inputMethodManager
                .hideSoftInputFromWindow(noteContentEditText.windowToken, 0)
        }
    }

    private fun saveEdit(holder: NoteCard, note: Note) {
        if (newNoteCreated) {
            newNoteCreated = false
        }
        val newNoteContent = holder.binding.noteContentEditText.text.toString()
        val newNote = note.copy(content = newNoteContent)
        turnOffEditMode(holder)
        callback.onNoteEdited(newNote)
    }
}

internal class NoteCard(val binding: NoteCardBinding) : RecyclerView.ViewHolder(binding.root)