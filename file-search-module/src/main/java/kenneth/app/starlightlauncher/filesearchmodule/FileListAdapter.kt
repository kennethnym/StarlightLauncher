package kenneth.app.starlightlauncher.filesearchmodule

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.filesearchmodule.databinding.FileListItemBinding
import kotlin.math.min

internal class FileListAdapter(
    private val context: Context,
    private val files: List<DocumentFile>,
) : RecyclerView.Adapter<FileListItem>() {
    private val visibleFiles =
        if (files.size <= INITIAL_LIST_ITEM_COUNT) files.toMutableList()
        else files.subList(0, INITIAL_LIST_ITEM_COUNT).toMutableList()

    val hasMore: Boolean
        get() = visibleFiles.size < files.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListItem {
        val binding =
            FileListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileListItem(binding)
    }

    override fun onBindViewHolder(holder: FileListItem, position: Int) {
        val currentFile = files[position]
        with(holder.binding) {
            file = currentFile
            root.setOnClickListener { openFile(currentFile) }
        }
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

    private fun openFile(documentFile: DocumentFile) {
        try {
            Log.d("FileListAdapter", "uri: ${documentFile.uri}")
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = documentFile.uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        } catch (ex: ActivityNotFoundException) {
            // no activity found to view the file.
            notifyUserCantOpenFile()
        }
    }

    private fun notifyUserCantOpenFile() {
        Toast.makeText(
            context, context.getString(R.string.no_app_to_open_file), Toast.LENGTH_LONG
        ).show()
    }
}

internal class FileListItem(internal val binding: FileListItemBinding) :
    RecyclerView.ViewHolder(binding.root)