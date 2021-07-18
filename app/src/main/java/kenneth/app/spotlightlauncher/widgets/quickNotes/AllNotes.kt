package kenneth.app.spotlightlauncher.widgets.quickNotes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.AllNotesBinding
import kenneth.app.spotlightlauncher.databinding.NoteCardBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.TimeAgo
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@ExperimentalTime
@AndroidEntryPoint
class AllNotes(context: Context) : FrameLayout(context) {
    @Inject
    lateinit var notesPreferenceManager: NotesPreferenceManager

    @Inject
    lateinit var noteCardListAdapter: NoteCardListAdapter

    private val binding = AllNotesBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)

        setPadding(0, topPadding, 0, 0)

        val notes = notesPreferenceManager.notes
        val hasNotes = notes.isNotEmpty()

        noteCardListAdapter.data = notes

        with(binding) {
            noteCardList.apply {
                isVisible = hasNotes
                adapter = noteCardListAdapter
                layoutManager = noteCardListAdapter.layoutManager
            }
            addNoteButton.setOnClickListener { addNote() }
        }
    }

    private fun addNote() {
        val emptyNote = Note(content = "")

        notesPreferenceManager.addNote(emptyNote)
        noteCardListAdapter
            .apply {
                data = notesPreferenceManager.notes
                hasNewItem = true
            }
            .also {
                val lastIndex = it.data.size - 1
                it.notifyItemInserted(lastIndex)
            }

        binding.noteCardListScrollView.run {
            val lastView = getChildAt(childCount - 1)
            val lastViewBottom = lastView.bottom + paddingBottom
            val amountToScroll = lastViewBottom - height - scrollY
            smoothScrollBy(0, amountToScroll)
        }
    }
}

@ExperimentalTime
class NoteCardListAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val timeAgo: TimeAgo,
    private val notesPreferenceManager: NotesPreferenceManager,
) : RecyclerViewDataAdapter<Note, NoteCard>() {
    override var data = listOf<Note>()

    /**
     * Indicates whether there is a newly-inserted note card.
     * If true, the note card will automatically enable edit mode. Afterwards,
     * this will be flipped back to false.
     */
    var hasNewItem = false

    override val layoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCard {
        val binding = NoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteCard(context, this, binding, timeAgo, notesPreferenceManager)
    }
}
