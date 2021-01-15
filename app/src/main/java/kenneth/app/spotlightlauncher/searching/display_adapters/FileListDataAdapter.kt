package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.model.stream.MediaStoreImageThumbLoader
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.dp
import kenneth.app.spotlightlauncher.views.BlurView
import kenneth.app.spotlightlauncher.views.TextButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val INITIAL_ITEM_COUNT = 5

object FileListDataAdapter :
    RecyclerViewDataAdapter<DocumentFile, FileListDataAdapter.ViewHolder>() {
    /**
     * The card view that is containing this RecyclerView
     */
    private lateinit var cardContainer: LinearLayout
    private lateinit var cardBlurBackground: BlurView
    private lateinit var showMoreButton: TextButton
    private lateinit var openSettingsButton: TextButton
    private lateinit var resultStatusLabel: TextView

    private lateinit var allData: List<DocumentFile>

    override val layoutManager: RecyclerView.LayoutManager
        get() = LinearLayoutManager(activity)

    override val recyclerView: RecyclerView
        get() = activity.findViewById(R.id.files_list)

    override fun getInstance(activity: Activity) = this.apply { this.activity = activity }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.files_list_item, parent, false) as LinearLayout

        return ViewHolder(listItemView, activity)
    }

    override fun displayData(data: List<DocumentFile>?) {
        super.displayData(data)

        val fileList = data

        findViews()

        cardContainer.isVisible = true
        cardBlurBackground
            .apply { isVisible = true }
            .also { it.startBlur() }

        when {
            data == null -> {
                recyclerView.isVisible = false
                showMoreButton.isVisible = false
                openSettingsButton.isVisible = true
                resultStatusLabel.apply {
                    isVisible = true
                    text = activity.getString(R.string.files_section_include_path_instruction)
                    setPadding(0, 0, 0, 0)
                }
            }
            data.isEmpty() -> {
                recyclerView.isVisible = false
                showMoreButton.isVisible = false
                openSettingsButton.isVisible = false
                resultStatusLabel.apply {
                    isVisible = true
                    text = activity.getString(R.string.files_section_no_result)
                    setPadding(0, 0, 0, 8.dp)
                }
            }
            else -> {
                recyclerView.isVisible = true
                showMoreButton.isVisible = true
                openSettingsButton.isVisible = false
                resultStatusLabel.isVisible = false
                showMoreButton.apply {
                    isVisible = true
                    setOnClickListener { showMoreFiles() }
                }

                allData = fileList!!
                this.data =
                    fileList.subList(0, min(fileList.size, INITIAL_ITEM_COUNT)).toMutableList()

                notifyDataSetChanged()
            }
        }
    }

    /**
     * Hides the views associated with this adapter.
     */
    fun hideFileList() {
        if (::cardContainer.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.apply {
                pauseBlur()
                isVisible = false
            }
            cardContainer.isVisible = false
        }
    }

    /**
     * Finds and stores all relevant views
     */
    private fun findViews() {
        with(activity) {
            if (!::cardContainer.isInitialized) {
                cardContainer = findViewById(R.id.files_section_card)
            }

            if (!::cardBlurBackground.isInitialized) {
                cardBlurBackground = findViewById(R.id.files_section_card_blur_background)
            }

            if (!::showMoreButton.isInitialized) {
                showMoreButton = findViewById(R.id.files_list_show_more_button)
            }

            if (!::openSettingsButton.isInitialized) {
                openSettingsButton = findViewById(R.id.open_settings_button)
            }

            if (!::resultStatusLabel.isInitialized) {
                resultStatusLabel = findViewById(R.id.files_section_result_status)
            }
        }
    }

    private fun showMoreFiles() {
        val currentItemCount = data.size
        val newItemCount = currentItemCount + INITIAL_ITEM_COUNT
        val totalItemCount = allData.size

        (data as MutableList).addAll(
            allData.subList(
                currentItemCount,
                min(totalItemCount, newItemCount)
            )
        )
        showMoreButton.isVisible = newItemCount < totalItemCount

        notifyItemRangeInserted(currentItemCount, INITIAL_ITEM_COUNT)
    }

    class ViewHolder(view: LinearLayout, activity: Activity) :
        RecyclerViewDataAdapter.ViewHolder<DocumentFile>(view, activity) {
        override fun bindWith(data: DocumentFile) {
            val file = data

            with(view) {
                findViewById<TextView>(R.id.files_list_item_file_name)
                    .text = file.name

                findViewById<LinearLayout>(R.id.files_list_item_container)
                    .setOnClickListener { openFile(file) }
            }
        }

        private fun getFileMimeType(file: DocumentFile): String? {
            val ext = MimeTypeMap.getFileExtensionFromUrl(file.uri.toString())
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }

        private fun openFile(file: DocumentFile) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = file.uri
                type = getFileMimeType(file)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                activity.startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
            }
        }
    }
}
