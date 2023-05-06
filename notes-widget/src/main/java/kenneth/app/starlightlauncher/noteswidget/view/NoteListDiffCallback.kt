package kenneth.app.starlightlauncher.noteswidget.view

import androidx.recyclerview.widget.DiffUtil
import kenneth.app.starlightlauncher.noteswidget.Note

internal class NoteListDiffCallback(
    private val oldList: List<Note>,
    private val newList: List<Note>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}