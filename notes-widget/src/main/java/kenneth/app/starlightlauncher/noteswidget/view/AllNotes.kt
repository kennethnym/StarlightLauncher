package kenneth.app.starlightlauncher.noteswidget.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.util.dp
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.databinding.AllNotesBinding

class AllNotes(context: Context) : FrameLayout(context) {
    private val binding = AllNotesBinding.inflate(LayoutInflater.from(context), this)
    private val listAdapter: AllNoteCardListAdapter

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            with(binding.noteCardListScrollView) {
                updatePadding(bottom = binding.addNoteButton.height + 24.dp)
                smoothScrollTo(0, 0)
            }
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )

        val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)

        with(binding) {
            addNoteButton.setOnClickListener { addNote() }
            noteCardListScrollView.apply {
                setPadding(0, topPadding, 0, addNoteButton.height)
                clipToPadding = false
            }
            noteCardList.apply {
                adapter = AllNoteCardListAdapter(context).also { listAdapter = it }
                layoutManager = LinearLayoutManager(context)
            }
        }

        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
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
