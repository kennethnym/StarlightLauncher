package kenneth.app.spotlightlauncher.searching.display_adapters

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val imageOrVideoMimeType = Regex("(^image/.+)|(^video/.+)")

object FileListDataAdapter :
    RecyclerViewDataAdapter<DocumentFile, FileListDataAdapter.ViewHolder>() {
    override fun getInstance(activity: MainActivity): FileListDataAdapter {
        this.activity = activity.also {
            it.findViewById<RecyclerView>(R.id.files_list)
                .apply {
                    layoutManager = LinearLayoutManager(it)
                    adapter = this@FileListDataAdapter
                }
        }

        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.files_list_item, parent, false) as LinearLayout

        return ViewHolder(listItemView, activity)
    }

    override fun displayData(data: List<DocumentFile>?) {
        val fileList = data

        with(activity) {
            findViewById<MaterialCardView>(R.id.files_section_card).visibility = View.VISIBLE

            val fileListRecyclerView = findViewById<RecyclerView>(R.id.files_list)

            when {
                data == null -> {
                    fileListRecyclerView.visibility = View.GONE
                    findViewById<Button>(R.id.open_settings_button).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.files_section_include_path_instruction)
                        setPadding(0, 0, 0, 0)
                    }
                }
                data.isEmpty() -> {
                    fileListRecyclerView.visibility = View.GONE
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.files_section_no_result)
                        setPadding(0, 0, 0, 8.toPx(activity.resources))
                    }
                }
                else -> {
                    fileListRecyclerView.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.files_section_result_status).visibility = View.GONE

                    this@FileListDataAdapter.data = fileList!!

                    notifyDataSetChanged()
                }
            }
        }
    }

    class ViewHolder(view: LinearLayout, activity: MainActivity) :
        RecyclerViewDataAdapter.ViewHolder<DocumentFile>(view, activity) {
        private val imagePreviewCoroutine = CoroutineScope(Dispatchers.IO)

        override fun bindWith(data: DocumentFile) {
            val file = data

            with(view) {
                findViewById<TextView>(R.id.files_list_item_file_name)
                    .text = file.name

                findViewById<LinearLayout>(R.id.files_list_item_container)
                    .setOnClickListener { openFile(file) }

                val mimeType = getFileMimeType(file)

                if (mimeType != null && imageOrVideoMimeType.matches(mimeType)) {
                    imagePreviewCoroutine.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            getUriPreview(
                                file.uri,
                                size = resources.getDimensionPixelSize(R.dimen.material_list_item_bigger_avatar_dimen)
                            )
                        }

                        findViewById<ImageView>(R.id.files_list_item_preview).setImageBitmap(bitmap)
                    }
                }
            }
        }

        private fun getUriPreview(uri: Uri, size: Int) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.contentResolver.loadThumbnail(
                    uri,
                    Size(size, size),
                    null
                )
            } else {
                val loader = CursorLoader(
                    activity,
                    uri,
                    arrayOf(MediaStore.Images.Media.DATA),
                    null,
                    null,
                    null
                )
                val cursor = loader.loadInBackground()
                val colIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                cursor.moveToFirst()

                MediaStore.Images.Thumbnails.getThumbnail(
                    activity.contentResolver,
                    cursor.getLong(colIndex),
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    null,
                )
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

            activity.startActivity(intent)
        }
    }
}
