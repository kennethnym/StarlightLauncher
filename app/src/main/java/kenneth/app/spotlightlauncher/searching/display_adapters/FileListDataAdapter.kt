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

private val imageOrVideoMimeType = Regex("(^image/.+)|(^video/.+)")

object FileListDataAdapter :
    RecyclerViewDataAdapter<DocumentFile, FileListDataAdapter.ViewHolder>() {
    /**
     * The card view that is containing this RecyclerView
     */
    private lateinit var cardContainer: LinearLayout
    private lateinit var cardBlurBackground: BlurView
    private lateinit var showMoreButton: TextButton

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

        with(activity) {
            cardContainer = findViewById<LinearLayout>(R.id.files_section_card).apply {
                visibility = View.VISIBLE
            }

            cardBlurBackground = findViewById<BlurView>(R.id.files_section_card_blur_background)
                .also { it.startBlur() }

            showMoreButton = findViewById(R.id.files_list_show_more_button)

            val fileListRecyclerView = findViewById<RecyclerView>(R.id.files_list)

            when {
                data == null -> {
                    fileListRecyclerView.isVisible = false
                    showMoreButton.isVisible = false
                    findViewById<TextButton>(R.id.open_settings_button).isVisible = true
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        isVisible = true
                        text = getString(R.string.files_section_include_path_instruction)
                        setPadding(0, 0, 0, 0)
                    }
                }
                data.isEmpty() -> {
                    fileListRecyclerView.isVisible = false
                    showMoreButton.isVisible = false
                    findViewById<TextButton>(R.id.open_settings_button).isVisible = false
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        isVisible = true
                        text = getString(R.string.files_section_no_result)
                        setPadding(0, 0, 0, 8.dp)
                    }
                }
                else -> {
                    fileListRecyclerView.isVisible = true
                    showMoreButton.isVisible = true
                    findViewById<TextButton>(R.id.open_settings_button).isVisible = false
                    findViewById<TextView>(R.id.files_section_result_status).isVisible = false
                    findViewById<TextButton>(R.id.files_list_show_more_button).apply {
                        isVisible = true
                        setOnClickListener { showMoreFiles() }
                    }

                    this@FileListDataAdapter.allData = fileList!!
                    this@FileListDataAdapter.data =
                        fileList.subList(0, min(fileList.size, INITIAL_ITEM_COUNT)).toMutableList()

                    notifyDataSetChanged()
                }
            }
        }
    }

    fun hideFileList() {
        if (::cardContainer.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.pauseBlur()
            cardContainer.isVisible = false
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
        private val imagePreviewCoroutine = CoroutineScope(Dispatchers.IO)

        override fun bindWith(data: DocumentFile) {
            val file = data

            with(view) {
                findViewById<TextView>(R.id.files_list_item_file_name)
                    .text = file.name

                findViewById<LinearLayout>(R.id.files_list_item_container)
                    .setOnClickListener { openFile(file) }
            }
        }

        private fun getUriPreview(uri: Uri, size: Int): Bitmap? {
            val cursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(
                view.context.contentResolver,
                uri,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null
            )

            return if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val thumbnailUri =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA))

                MediaStore.Images.Media.getBitmap(
                    view.context.contentResolver,
                    Uri.parse(thumbnailUri)
                )
            } else null
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
