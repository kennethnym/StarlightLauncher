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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.documentfile.provider.DocumentFile
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.toPx

private val imageOrVideoMimeType = Regex("(^image/.+)|(^video/.+)")

class FileListAdapter(activity: MainActivity) :
    SectionResultAdapter<List<DocumentFile>?, FileListAdapter.ListItem>(activity) {
    companion object {
        fun initializeWith(activity: MainActivity) = FileListAdapter(activity).also {
            activity.findViewById<RecyclerView>(R.id.files_list).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = it
            }
        }
    }

    lateinit var fileList: List<DocumentFile>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItem {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.files_list_item, parent, false) as ConstraintLayout

        val holder = ListItem(listItemView)

        return holder
    }

    override fun onBindViewHolder(holder: ListItem, position: Int) {
        val file = fileList[position]

        with(holder.layout) {
            findViewById<TextView>(R.id.files_list_item_file_name)
                .text = file.name

            findViewById<LinearLayout>(R.id.files_list_item_container)
                .setOnClickListener { openFile(file) }

            val mimeType = getFileMimeType(file)

            if (mimeType != null && imageOrVideoMimeType.matches(mimeType)) {
                val bitmap = getUriPreview(
                    file.uri,
                    size = resources.getDimensionPixelSize(R.dimen.material_list_item_avatar_dimen)
                )

                findViewById<ImageView>(R.id.files_list_item_preview).setImageBitmap(bitmap)
            }
        }
    }

    override fun getItemCount() = fileList.size

    override fun displayResult(result: List<DocumentFile>?) {
        with(activity) {
            findViewById<MaterialCardView>(R.id.files_section_card).visibility = View.VISIBLE

            when {
                result == null -> {
                    findViewById<RecyclerView>(R.id.files_list).visibility = View.GONE
                    findViewById<Button>(R.id.open_settings_button).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.files_section_include_path_instruction)
                        setPadding(0, 0, 0, 0)
                    }
                }
                result.isEmpty() -> {
                    findViewById<RecyclerView>(R.id.files_list).visibility = View.GONE
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.files_section_no_result)
                        setPadding(0, 0, 0, 8.toPx(activity.resources))
                    }
                }
                else -> {
                    findViewById<RecyclerView>(R.id.files_list).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.files_section_result_status).visibility = View.GONE

                    fileList = result

                    notifyDataSetChanged()
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

    class ListItem(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)
}