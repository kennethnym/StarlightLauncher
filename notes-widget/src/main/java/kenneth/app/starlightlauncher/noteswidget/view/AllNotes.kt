package kenneth.app.starlightlauncher.noteswidget.view

import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.dp
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.databinding.AllNotesBinding

internal class AllNotesFragment(val launcher: StarlightLauncherApi) :
    Fragment(),
    NoteListChangedCallback {
    private var binding: AllNotesBinding? = null
    private var listAdapter: AllNoteCardListAdapter? = null

    private val viewModel: AllNotesPageViewModel by viewModels { AllNotesPageViewModel.Factory }

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val binding = this@AllNotesFragment.binding ?: return
            with(binding.noteCardListScrollView) {
                updatePadding(
                    bottom = paddingBottom + binding.addNoteButton.height + 24.dp
                )

                // fix for note list scrolls to bottom by itself on first render
                post {
                    scrollTo(0, 0)
                    TransitionManager.beginDelayedTransition(this, Fade().apply {
                        duration = 200
                        addTarget(binding.noteCardListScrollView)
                    })
                    visibility = View.VISIBLE
                }
            }
            binding.root.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        AllNotesBinding.inflate(LayoutInflater.from(context)).run {
            binding = this

            val topPadding = resources.getDimensionPixelOffset(R.dimen.overlay_padding_top)

            addNoteButton.setOnClickListener { addNote() }

            noteCardListScrollView.apply {
                setPadding(0, topPadding, 0, addNoteButton.height)
                // initially, for some unknown reason, the note list will be scrolled
                // to the bottom on first render
                // the fix is to call scrollTo(0, 0) on the scroll view after global layout
                // but it will look janky to the user, so we wait until
                // the scroll view is scrolled to 0, 0 before making it visible to avoid the visual jank
                visibility = View.INVISIBLE
                clipToPadding = false
            }

            noteCardList.apply {
                adapter = AllNoteCardListAdapter(
                    launcher.context,
                    this@AllNotesFragment
                ).also {
                    listAdapter = it
                }
                layoutManager = LinearLayoutManager(context)
            }

            root.setOnApplyWindowInsetsListener { _, windowInsets ->
                noteCardListScrollView.updatePadding(
                    bottom = noteCardListScrollView.paddingBottom +
                            WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
                                .getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                )
                windowInsets
            }

            root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.notes.observe(viewLifecycleOwner) {
            listAdapter?.let { listAdapter ->
                val oldNotes = listAdapter.notes
                listAdapter.notes = it
                DiffUtil.calculateDiff(NoteListDiffCallback(oldNotes, it))
                    .dispatchUpdatesTo(listAdapter)
            }
        }
    }

    override fun onNoteDeleted(deletedNote: Note) {
        viewModel.deleteNote(deletedNote)
    }

    override fun onNoteEdited(editedNote: Note) {
        viewModel.editNote(editedNote)
    }

    private fun addNote() {
        listAdapter?.addNote()
        viewModel.addNote()
        binding?.noteCardListScrollView?.run {
            val lastView = getChildAt(childCount - 1)
            val lastViewBottom = lastView.bottom + paddingBottom
            val amountToScroll = lastViewBottom - height - scrollY
            smoothScrollBy(0, amountToScroll)
        }
    }
}
