package kenneth.app.starlightlauncher.noteswidget.view

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.databinding.QuickNoteListItemBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NoteListModified
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences

internal class QuickNoteListAdapter(context: Context) : RecyclerView.Adapter<QuickNoteListItem>() {
    private val prefs = NotesWidgetPreferences.getInstance(context)

    private var notes = prefs.notes.toMutableList()

    private var hasPendingOperations = false

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        with(prefs) {
            addNoteListModifiedListener(::onNoteListChanged)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickNoteListItem {
        val binding =
            QuickNoteListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickNoteListItem(binding)
    }

    override fun onBindViewHolder(holder: QuickNoteListItem, position: Int) {
        val note = notes[position]
        with(holder.binding) {
            quickNotesListItemContent.text = note.content
            quickNotesListItemDeleteButton.setOnClickListener {
                deleteNote(note)
            }
        }
    }

    override fun getItemCount(): Int = notes.size

    private fun onNoteListChanged(event: NoteListModified) {
        when (event) {
            is NoteListModified.NoteRemoved -> {
                val index = notes.indexOfFirst { it.id == event.note.id }
                notes.removeAt(index)
                notifyItemRemoved(index)
                hasPendingOperations = false
            }
            is NoteListModified.NoteAdded -> {
                notes.add(event.note)
                notifyItemInserted(notes.size - 1)
                hasPendingOperations = false
            }
            is NoteListModified.NoteChanged -> {
                val index = notes.indexOfFirst { it.id == event.note.id }
                if (index >= 0) {
                    notes[index] = event.note
                    notifyItemChanged(index)
                    hasPendingOperations = false
                }
            }
            is NoteListModified.ListChanged -> {
                notes.clear()
                notes += event.notes
                notifyItemRangeChanged(0, notes.size)
            }
        }
    }

    private fun deleteNote(note: Note) {
        Log.d("Starlight", "deleting note")
        Log.d("Starlight", "has pending operations? $hasPendingOperations")
        if (!hasPendingOperations) {
            hasPendingOperations = true
            prefs.deleteNote(note)
        }
    }
}

internal class QuickNoteListItem(
    val binding: QuickNoteListItemBinding,
) : RecyclerView.ViewHolder(binding.root)