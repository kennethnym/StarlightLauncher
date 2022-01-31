package kenneth.app.starlightlauncher.filesearchmodule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.filesearchmodule.databinding.FileListItemBinding
import kotlin.math.min

internal class FileListAdapter(
    private val files: List<DocumentFile>
) : RecyclerView.Adapter<FileListItem>() {
    private val visibleFiles = files.subList(0, INITIAL_LIST_ITEM_COUNT).toMutableList()

    val hasMore: Boolean
        get() = visibleFiles.size < files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListItem {
        val binding = FileListItemBinding.inflate(LayoutInflater.from(parent.context))
        return FileListItem(binding)
    }

    override fun onBindViewHolder(holder: FileListItem, position: Int) {
        val file = files[position]
    }

    override fun getItemCount(): Int = visibleFiles.size

    fun showMore() {
        // the total number of apps that can be displayed
        val totalItemCount = files.size
        // the current number of items in the grid
        val currentItemCount = visibleFiles.size
        // the number of new apps to be added to the grid
        val addedItemsCount = min(INITIAL_LIST_ITEM_COUNT, totalItemCount - currentItemCount)
        // the total number of items after the items are added
        val newItemCount = currentItemCount + addedItemsCount

        visibleFiles.addAll(
            files.subList(currentItemCount, min(totalItemCount, newItemCount))
        )

        notifyItemRangeChanged(currentItemCount, addedItemsCount)
    }
}

internal class FileListItem(internal val binding: FileListItemBinding) :
    RecyclerView.ViewHolder(binding.root)