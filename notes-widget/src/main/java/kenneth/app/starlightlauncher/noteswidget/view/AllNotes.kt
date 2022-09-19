package kenneth.app.starlightlauncher.noteswidget.view

import android.view.LayoutInflater
import android.view.ViewTreeObserver
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.dp
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.databinding.AllNotesBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class AllNotesFragment(launcher: StarlightLauncherApi) :
    Fragment(),
    NoteListChangedCallback {
    private val binding = AllNotesBinding.inflate(LayoutInflater.from(context))
    private val prefs = NotesWidgetPreferences.getInstance(launcher)
    private val listAdapter: AllNoteCardListAdapter

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            with(binding.noteCardListScrollView) {
                updatePadding(bottom = binding.addNoteButton.height + 24.dp)
                smoothScrollTo(0, 0)
            }
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    init {
        val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)

        with(binding) {
            addNoteButton.setOnClickListener { addNote() }
            noteCardListScrollView.apply {
                setPadding(0, topPadding, 0, addNoteButton.height)
                clipToPadding = false
            }
            noteCardList.apply {
                adapter = AllNoteCardListAdapter(
                    launcher, this@AllNotesFragment
                ).also {
                    listAdapter = it
                }
                layoutManager = LinearLayoutManager(context)
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        lifecycleScope.launch { subscribeToNoteList() }
    }

    override fun onNoteDeleted(deletedNote: Note) {
        lifecycleScope.launch {
            prefs.deleteNote(deletedNote)
        }
    }

    override fun onNoteEdited(editedNote: Note) {
        lifecycleScope.launch {
            prefs.editNote(editedNote)
        }
    }

    private suspend fun subscribeToNoteList() {
        prefs.notes.collect {
            val oldNotes = listAdapter.notes
            listAdapter.notes = it
            DiffUtil.calculateDiff(NoteListDiffCallback(oldNotes, it))
                .dispatchUpdatesTo(listAdapter)
        }
    }

    private fun addNote() {
        listAdapter.addNote()
        lifecycleScope.launch {
            prefs.addNote(Note())
        }
        binding.noteCardListScrollView.run {
            val lastView = getChildAt(childCount - 1)
            val lastViewBottom = lastView.bottom + paddingBottom
            val amountToScroll = lastViewBottom - height - scrollY
            smoothScrollBy(0, amountToScroll)
        }
    }
}
