package kenneth.app.spotlightlauncher.widgets.quickNotes

import kenneth.app.spotlightlauncher.databinding.NoteCardBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.TimeAgo
import kotlin.time.ExperimentalTime

class NoteCard(
    override val binding: NoteCardBinding,
    private val timeAgo: TimeAgo
) :
    RecyclerViewDataAdapter.ViewHolder<Note>(binding) {
    private lateinit var note: Note

    @ExperimentalTime
    override fun bindWith(data: Note) {
        note = data
        with(binding) {
            noteTimestamp.text = timeAgo.prettify(note.createdOn)
            noteContent.text = note.content
        }
    }
}