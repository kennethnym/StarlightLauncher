package kenneth.app.starlightlauncher.noteswidget.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.databinding.AllNotesBinding

class AllNotes(context: Context) : FrameLayout(context) {
    private val binding = AllNotesBinding.inflate(LayoutInflater.from(context), this)
    private val listAdapter: AllNoteCardListAdapter

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )

        val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)
        setPadding(0, topPadding, 0, 0)

        with(binding) {
            noteCardList.apply {
                adapter = AllNoteCardListAdapter(context).also { listAdapter = it }
                layoutManager = LinearLayoutManager(context)
            }
            addNoteButton.setOnClickListener { addNote() }
        }
    }

    private fun addNote() {
        listAdapter.addNote()
        binding.noteCardListScrollView.run {
            val lastView = getChildAt(childCount - 1)
            val lastViewBottom = lastView.bottom + paddingBottom
            val amountToScroll = lastViewBottom - height - scrollY
            smoothScrollBy(0, amountToScroll)
        }
    }
}
