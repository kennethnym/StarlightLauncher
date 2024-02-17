package kenneth.app.starlightlauncher.noteswidget.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.databinding.QuickNoteListItemBinding

interface QuickNoteListAdapterCallback {
    fun onNoteDeleted(deletedNote: Note)
}

internal class QuickNoteListAdapter(
    private val callback: QuickNoteListAdapterCallback
) :
    RecyclerView.Adapter<QuickNoteListItem>() {
    private var notes = listOf<Note>()

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
                callback.onNoteDeleted(note)
            }
        }
    }

    override fun getItemCount(): Int = notes.size

    fun update(newNotes: List<Note>) {
        val oldNotes = notes
        notes = newNotes
        DiffUtil.calculateDiff(NoteListDiffCallback(oldNotes, newNotes))
            .dispatchUpdatesTo(this)
    }
}

internal class QuickNoteListItem(
    val binding: QuickNoteListItemBinding,
) : RecyclerView.ViewHolder(binding.root)