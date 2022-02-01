package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppGridItemBinding
import kenneth.app.starlightlauncher.appsearchmodule.view.AppOptionMenu
import kotlin.math.min


internal class AppGridAdapter(
    private val context: Context,
    apps: AppList,
    private val launcher: StarlightLauncherApi,
    private val initialVisibleItemCount: Int = 0,
) : RecyclerView.Adapter<AppGridItem>() {
    private val apps = apps.toMutableList()

    private val visibleApps =
        if (initialVisibleItemCount > 0)
            apps.subList(0, initialVisibleItemCount).toMutableList()
        else
            apps.toMutableList()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appSearchModulePreferences = AppSearchModulePreferences.getInstance(context)

    private lateinit var selectedApp: ActivityInfo

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppGridItem {
        val binding = AppGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppGridItem(binding)
    }

    override fun onBindViewHolder(holder: AppGridItem, position: Int) {
        val app = apps[position]
        val appName = app.loadLabel(launcher.context.packageManager)

        with(holder.binding) {
            appIcon.apply {
                contentDescription =
                    context.getString(R.string.app_icon_content_description, appName)
                setImageBitmap(launcher.getIconPack().getIconOf(app))
            }

            if (appSearchModulePreferences.shouldShowAppLabels) {
                appLabel.apply {
                    isVisible = true
                    text = appName
                }
            } else {
                appLabel.isVisible = false
            }

            with(root) {
                setOnLongClickListener {
                    selectedApp = app
                    showAppOptionMenu()
                }

                setOnClickListener {
                    selectedApp = app
                    openSelectedApp()
                }
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(holder)
    }

    override fun getItemCount(): Int = apps.size

    fun addAppToGrid(app: ActivityInfo, index: Int) {
        apps += app
        notifyItemInserted(index)
    }

    fun removeAppFromGrid(index: Int) {
        apps.removeAt(index)
        notifyItemRemoved(index)
    }

    fun showMore() {
        if (hasMore()) {
            // the total number of apps that can be displayed
            val totalItemCount = apps.size
            // the current number of items in the grid
            val currentItemCount = visibleApps.size
            // the number of new apps to be added to the grid
            val addedItemsCount = min(initialVisibleItemCount, totalItemCount - currentItemCount)
            // the total number of items after the items are added
            val newItemCount = currentItemCount + addedItemsCount

            visibleApps.addAll(
                apps.subList(
                    currentItemCount,
                    min(totalItemCount, newItemCount)
                )
            )

            notifyItemRangeInserted(currentItemCount, addedItemsCount)
        }
    }

    fun hasMore() = visibleApps.size < apps.size

    private fun showAppOptionMenu(): Boolean {
        launcher.showOptionMenu { menu -> AppOptionMenu(context, selectedApp, menu) }
        return true
    }

    private fun openSelectedApp() {
        launcher.context.startActivity(
            launcher.context.packageManager.getLaunchIntentForPackage(selectedApp.packageName)
        )
    }
}

internal class AppGridItem(internal val binding: AppGridItemBinding) :
    RecyclerView.ViewHolder(binding.root),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = AppSearchModulePreferences.getInstance(itemView.context)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            prefs.keys.showAppLabels -> {
                binding.appLabel.isVisible = prefs.shouldShowAppLabels
            }
        }
    }
}