package kenneth.app.spotlightlauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.icu.text.Transliterator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class AppsGridAdapter(
    private val activity: MainActivity,
    private val packageManager: PackageManager
) :
    RecyclerView.Adapter<AppsGridAdapter.GridItem>() {
    lateinit var appList: List<ResolveInfo>

    private fun onAppGridItemClicked(holder: GridItem) {
        // launch the clicked app
        val position = holder.adapterPosition

        if (position != RecyclerView.NO_POSITION) {
            val app = appList[position]
            val appActivity = app.activityInfo
            val componentName =
                ComponentName(appActivity.applicationInfo.packageName, appActivity.name)

            val intent = Intent(Intent.ACTION_MAIN).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                component = componentName
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            activity.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridItem {
        val gridItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.apps_grid_item, parent, false) as ConstraintLayout

        val holder = GridItem(gridItem)

        gridItem.setOnClickListener {
            onAppGridItemClicked(holder)
        }

        return holder
    }

    override fun onBindViewHolder(holder: GridItem, position: Int) {
        val app = appList[position]
        val appName = app.loadLabel(packageManager)

        holder.layout.apply {
            findViewById<ImageView>(R.id.app_icon).apply {
                contentDescription = "App icon for $appName"
                setImageDrawable(app.loadIcon(packageManager))
            }

            findViewById<TextView>(R.id.app_label).text = appName
        }
    }

    override fun getItemCount() = appList.size

    class GridItem(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)
}
