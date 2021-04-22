package kenneth.app.spotlightlauncher.widgets.quickNotes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.AllNotesBinding
import kenneth.app.spotlightlauncher.databinding.NoteCardBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.TimeAgo
import kenneth.app.spotlightlauncher.utils.dp
import javax.inject.Inject

@AndroidEntryPoint
class AllNotes(context: Context) : LinearLayout(context) {
    @Inject
    lateinit var notesPreferenceManager: NotesPreferenceManager

    @Inject
    lateinit var noteCardListAdapter: NoteCardListAdapter

    private val binding = AllNotesBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)

        setPadding(0, topPadding, 0, 0)

        val notes = notesPreferenceManager.notes
        val hasNotes = notes.isNotEmpty()

        noteCardListAdapter.data = notes
        binding.noteCardList.apply {
            isVisible = hasNotes
            adapter = noteCardListAdapter
            layoutManager = noteCardListAdapter.layoutManager
        }
    }
}

class NoteCardListAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val timeAgo: TimeAgo
) : RecyclerViewDataAdapter<Note, NoteCard>() {
    override val layoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCard {
        val binding = NoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteCard(binding, timeAgo)
    }
}
