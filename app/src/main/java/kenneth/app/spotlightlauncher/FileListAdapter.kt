package kenneth.app.spotlightlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.utils.toPx
import java.io.File

class FileListAdapter(activity: MainActivity) :
    SectionResultAdapter<List<File>?, FileListAdapter.ListItem>(activity) {
    companion object {
        fun initializeWith(activity: MainActivity) = FileListAdapter(activity).also {
            activity.findViewById<RecyclerView>(R.id.files_list).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = it
            }
        }
    }

    lateinit var fileList: List<File>

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

            findViewById<TextView>(R.id.files_list_item_file_path)
                .text = file.path
        }
    }

    override fun getItemCount() = fileList.size

    override fun displayResult(result: List<File>?) {
        with(activity) {
            findViewById<MaterialCardView>(R.id.files_section_card).visibility = View.VISIBLE
            findViewById<Button>(R.id.open_settings_button).visibility = View.VISIBLE

            when {
                result == null -> {
                    findViewById<RecyclerView>(R.id.files_list).visibility = View.GONE
                    findViewById<TextView>(R.id.files_section_result_status).apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.files_section_grant_permission)
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

    class ListItem(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)
}