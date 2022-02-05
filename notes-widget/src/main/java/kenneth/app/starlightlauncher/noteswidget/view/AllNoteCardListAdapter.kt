package kenneth.app.starlightlauncher.noteswidget.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.databinding.NoteCardBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NoteListChanged
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences

internal class AllNoteCardListAdapter(private val context: Context) :
    RecyclerView.Adapter<NoteCard>() {
    private val prefs = NotesWidgetPreferences.getInstance(context)
    private val notes = mutableListOf<Note>()

    private var newNoteCreated = false

    init {
        notes += prefs.notes
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        prefs.addNoteListChangedListener(::onNoteListChanged)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCard {
        val binding = NoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteCard(binding)
    }

    override fun onBindViewHolder(holder: NoteCard, position: Int) {
        val currentNote = notes[position]
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
                    prefs.deleteNote(currentNote)
                } else {
                    turnOffEditMode(holder)
                }
            }
            noteCardSaveEdit.setOnClickListener { saveEdit(holder, currentNote) }
        }
    }

    override fun getItemCount(): Int = notes.size

    internal fun addNote() {
        val emptyNote = Note()
        newNoteCreated = true
        prefs.addNote(emptyNote)
    }

    private fun onNoteListChanged(event: NoteListChanged) {
        when (event.status) {
            NoteListChanged.Status.NOTE_ADDED -> {
                notes += event.note
                notifyItemInserted(notes.size - 1)
            }
            NoteListChanged.Status.NOTE_CHANGED -> {
                val index = notes.indexOfFirst { it.id == event.note.id }
                if (index >= 0) {
                    notes[index] = event.note
                    notifyItemChanged(index)
                }
            }
            NoteListChanged.Status.NOTE_REMOVED -> {
                val index = notes.indexOfFirst { it.id == event.note.id }
                if (index >= 0) {
                    notes.removeAt(index)
                    notifyItemRemoved(index)
                }
            }
        }
    }

    private fun deleteNote(holder: NoteCard, note: Note) {
        holder.binding.areControlsEnabled = false
        prefs.deleteNote(note)
    }

    private fun turnOnEditMode(holder: NoteCard) {
        with(holder.binding) {
            inEditMode = true
            if (noteContentEditText.requestFocus()) {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(noteContentEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun turnOffEditMode(holder: NoteCard) {
        with(holder.binding) {
            inEditMode = false
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(noteContentEditText.windowToken, 0)
        }
    }

    private fun saveEdit(holder: NoteCard, note: Note) {
        if (newNoteCreated) {
            newNoteCreated = false
        }
        val newNoteContent = holder.binding.noteContentEditText.text.toString()
        val newNote = note.copy(content = newNoteContent)
        prefs.editNote(newNote)
        turnOffEditMode(holder)
    }
}

internal class NoteCard(val binding: NoteCardBinding) : RecyclerView.ViewHolder(binding.root)