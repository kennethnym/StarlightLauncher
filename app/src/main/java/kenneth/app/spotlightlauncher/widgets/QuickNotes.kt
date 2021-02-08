package kenneth.app.spotlightlauncher.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.databinding.QuickNotesLayoutBinding
import kenneth.app.spotlightlauncher.databinding.QuickNotesListItemBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import javax.inject.Inject

@AndroidEntryPoint
class QuickNotes(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var notesPreferenceManager: NotesPreferenceManager

    @Inject
    lateinit var noteListAdapter: NoteListAdapter

    private val binding: QuickNotesLayoutBinding =
        QuickNotesLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        with(binding) {
            addNoteButton.setOnClickListener { addNote() }

            addNotesEditText.addTextChangedListener {
                addNoteButton.isInvisible = it?.isBlank() == true
            }

            val savedNotes = notesPreferenceManager.notes

            noteListAdapter.data = savedNotes

            if (savedNotes.isNotEmpty()) {
                quickNotesList.apply {
                    isVisible = true
                    adapter = noteListAdapter
                    layoutManager = noteListAdapter.layoutManager
                }
            }

            quickNotesWidgetBlurBackground.startBlur()
        }
    }

    private fun addNote() {
        binding.addNotesEditText.text?.let {
            if (it.isNotBlank()) {
                val newNote = Note(content = it.toString(), dueDateTimestamp = null)
                noteListAdapter.apply {
                    data = data + newNote
                    notifyItemInserted(data.size)
                }
                notesPreferenceManager.addNote(newNote)
            }
        }
    }
}

class NoteListAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val notesPreferenceManager: NotesPreferenceManager,
) : RecyclerViewDataAdapter<Note, NoteListItem>() {
    override val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteListItem {
        val binding =
            QuickNotesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return NoteListItem(this, binding, notesPreferenceManager)
    }
}

/**
 * An item in [NoteListAdapter].
 * @param adapter The [NoteListAdapter] this is in.
 * @param binding The view binding of the layout of this item
 * @param notesPreferenceManager The instance of [NotesPreferenceManager] this should use to modify the list of notes
 */
class NoteListItem(
    private val adapter: NoteListAdapter,
    private val binding: QuickNotesListItemBinding,
    private val notesPreferenceManager: NotesPreferenceManager
) :
    RecyclerViewDataAdapter.ViewHolder<Note>(binding.root) {

    override fun bindWith(data: Note) {
        with(binding) {
            quickNotesListItemContent.text = data.content
            quickNotesListItemDeleteButton.setOnClickListener {
                notesPreferenceManager.deleteNote(data)
            }
        }
    }
}
