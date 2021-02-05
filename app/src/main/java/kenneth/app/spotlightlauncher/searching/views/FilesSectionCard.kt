package kenneth.app.spotlightlauncher.searching.views

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.FileListItemBinding
import kenneth.app.spotlightlauncher.databinding.FilesSectionCardBinding
import kenneth.app.spotlightlauncher.prefs.SettingsActivity
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.dp
import javax.inject.Inject
import kotlin.math.min

private const val INITIAL_ITEM_COUNT = 5

@AndroidEntryPoint
class FilesSectionCard(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var fileListAdapter: FileListAdapter

    private val binding = FilesSectionCardBinding.inflate(LayoutInflater.from(context), this, true)

    private val settingsIntent = Intent(context, SettingsActivity::class.java)

    private lateinit var files: List<DocumentFile>

    init {
        binding.openSettingsButton.setOnClickListener {
            openSettings()
        }
    }

    fun display(files: List<DocumentFile>?) {
        isVisible = true
        binding.filesSectionCardContainer.isVisible = true
        binding.filesSectionCardBlurBackground.startBlur()

        when {
            files == null -> {
                with(binding) {
                    filesList.isVisible = false
                    filesListShowMoreButton.isVisible = false
                    openSettingsButton.isVisible = true
                    filesSectionResultStatus.apply {
                        isVisible = true
                        text = context.getString(R.string.files_section_include_path_instruction)
                        setPadding(0, 0, 0, 0)
                    }
                }
            }
            files.isEmpty() -> {
                with(binding) {
                    filesList.isVisible = false
                    filesListShowMoreButton.isVisible = false
                    openSettingsButton.isVisible = false
                    filesSectionResultStatus.apply {
                        isVisible = true
                        text = context.getString(R.string.files_section_no_result)
                        setPadding(0, 0, 0, 8.dp)
                    }
                }
            }
            else -> {
                this.files = files

                val isPaginationRequired = files.size > INITIAL_ITEM_COUNT

                fileListAdapter.data =
                    if (isPaginationRequired)
                        files.subList(0, INITIAL_ITEM_COUNT)
                    else files

                with(binding) {
                    filesList.apply {
                        isVisible = true
                        adapter = fileListAdapter
                        layoutManager = fileListAdapter.layoutManager
                    }
                    filesListShowMoreButton.isVisible = isPaginationRequired
                    openSettingsButton.isVisible = false
                    filesSectionResultStatus.isVisible = false

                    filesListShowMoreButton.apply {
                        isVisible = true
                        setOnClickListener { showMoreFiles() }
                    }
                }
            }
        }
    }

    /**
     * Hides this card in the search result page.
     */
    fun hide() {
        binding.filesSectionCardContainer.isVisible = false
        isVisible = false
        binding.filesSectionCardBlurBackground.pauseBlur()
    }

    /**
     * Shows more files in the list.
     */
    private fun showMoreFiles() {
        // the total number of items that can be displayed
        val totalItemCount = files.size
        // the current number of items on the list
        val currentItemCount = fileListAdapter.itemCount
        // the number of new items added to the list
        val addedItemsCount = min(INITIAL_ITEM_COUNT, totalItemCount - currentItemCount)
        // the total number of items after the items are added
        val newItemCount = currentItemCount + addedItemsCount

        fileListAdapter.data += files.subList(
            currentItemCount,
            min(totalItemCount, newItemCount)
        )

        binding.filesListShowMoreButton.isVisible = newItemCount < totalItemCount

        fileListAdapter.notifyItemRangeInserted(currentItemCount, addedItemsCount)
    }

    private fun openSettings() {
        context.startActivity(settingsIntent);
    }
}

class FileListAdapter @Inject constructor(
    @ActivityContext private val context: Context
) : RecyclerViewDataAdapter<DocumentFile, FileListItem>() {
    override val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListItem {
        val binding =
            FileListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileListItem(binding)
    }
}

/**
 * The ViewHolder that displays a file item in [FileListAdapter]
 * @param binding The view binding of the underlying view.
 */
class FileListItem(private val binding: FileListItemBinding) :
    RecyclerViewDataAdapter.ViewHolder<DocumentFile>(binding.root) {
    /**
     * The file that is bound to this ViewHolder
     */
    private lateinit var file: DocumentFile

    override fun bindWith(data: DocumentFile) {
        file = data

        with(binding) {
            fileListItemFileName.text = file.name
            root.setOnClickListener { openFile() }
        }
    }

    private fun getFileMimeType(): String? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(file.uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = file.uri
            type = getFileMimeType()
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            startActivity(itemView.context, intent, null)
        } catch (ex: ActivityNotFoundException) {

        }
    }
}