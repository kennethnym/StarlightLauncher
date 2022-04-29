package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppGridItemBinding
import kenneth.app.starlightlauncher.appsearchmodule.view.AppOptionMenu
import kotlin.math.min

internal class AppGridAdapter(
    private val context: Context,
    apps: AppList,
    private val launcher: StarlightLauncherApi,
    private var shouldShowAppNames: Boolean,
    /**
     * Whether all apps should be shown at once. Defaults to false.
     */
    private val showAllApps: Boolean = false,
    /**
     * Whether an option menu should be shown when an item is long pressed. Defaults to true.
     */
    private val enableLongPressMenu: Boolean = true,
) : RecyclerView.Adapter<AppGridItem>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val apps = apps.toMutableList()

    private val visibleApps = mutableListOf<LauncherActivityInfo>()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appSearchModulePreferences = AppSearchModulePreferences.getInstance(context)
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private lateinit var selectedApp: LauncherActivityInfo
    private var recyclerView: RecyclerView? = null
    private var gridSpanCount = 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        this.recyclerView = recyclerView
        recyclerView.layoutManager.let {
            if (it is GridLayoutManager) {
                visibleApps +=
                    when {
                        showAllApps || apps.size <= it.spanCount -> apps
                        else -> apps.subList(0, it.spanCount)
                    }
                gridSpanCount = it.spanCount
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        this.recyclerView = null
        visibleApps.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppGridItem {
        val binding = AppGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppGridItem(binding)
    }

    override fun onBindViewHolder(holder: AppGridItem, position: Int) {
        val app = apps[position]
        val appName = app.label

        with(holder.binding) {
            appIcon.apply {
                contentDescription =
                    context.getString(R.string.app_icon_content_description, appName)
                setImageBitmap(launcher.getIconPack().getIconOf(app))
            }

            if (shouldShowAppNames) {
                appLabel.apply {
                    isVisible = true
                    text = appName
                }
            } else {
                appLabel.isVisible = false
            }

            with(root) {
                if (enableLongPressMenu) {
                    setOnLongClickListener {
                        selectedApp = app
                        showAppOptionMenu()
                    }
                }

                setOnClickListener {
                    selectedApp = app
                    openSelectedApp(holder.binding.appIcon)
                }
            }
        }
    }

    override fun getItemCount(): Int = visibleApps.size

    fun addAppToGrid(app: LauncherActivityInfo) {
        apps += app
        if (showAllApps) {
            visibleApps += app
        }
        notifyItemInserted(itemCount - 1)
    }

    fun removeAppFromGrid(index: Int) {
        apps.removeAt(index)
        if (index < visibleApps.size) {
            visibleApps.removeAt(index)
        }
        notifyItemRemoved(index)
    }

    fun showMore() {
        if (hasMore()) {
            // the total number of apps that can be displayed
            val totalItemCount = apps.size
            // the current number of items in the grid
            val currentItemCount = visibleApps.size
            // the number of new apps to be added to the grid
            val addedItemsCount = min(gridSpanCount, totalItemCount - currentItemCount)
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

    fun showAppLabels() {
        recyclerView?.let {
            for (i in 0 until it.childCount) {
                it.getChildViewHolder(it.getChildAt(i)).run {
                    if (this is AppGridItem)
                        binding.appLabel.apply {
                            text = apps[i].label
                            isVisible = true
                        }
                }
            }
        }
        shouldShowAppNames = true
    }

    fun hideAppLabels() {
        recyclerView?.let {
            for (i in 0 until it.childCount) {
                it.getChildViewHolder(it.getChildAt(i)).run {
                    if (this is AppGridItem)
                        binding.appLabel.isVisible = false
                }
            }
        }
        shouldShowAppNames = false
    }

    /**
     * Shows an option menu for the app at the given [position].
     */
    fun showAppOptionMenuAtPosition(position: Int) {
        recyclerView?.findViewHolderForAdapterPosition(position)?.let {
            selectedApp = apps[position]
            showAppOptionMenu()
        }
    }

    private fun showAppOptionMenu(): Boolean {
        launcher.showOptionMenu { menu -> AppOptionMenu(context, selectedApp, menu) }
        return true
    }

    private fun openSelectedApp(sourceIconView: View) {
        val sourceBounds = Rect().run {
            sourceIconView.getGlobalVisibleRect(this)
            this
        }

        launcherApps.startMainActivity(
            selectedApp.componentName,
            Process.myUserHandle(),
            sourceBounds,
            null
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            appSearchModulePreferences.keys.showAppNames -> {
                if (appSearchModulePreferences.shouldShowAppNames) {
                    showAppLabels()
                } else {
                    hideAppLabels()
                }
            }
        }
    }
}

internal class AppGridItem(internal val binding: AppGridItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

}
