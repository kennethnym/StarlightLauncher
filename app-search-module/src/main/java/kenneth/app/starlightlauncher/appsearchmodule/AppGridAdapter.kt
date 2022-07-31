package kenneth.app.starlightlauncher.appsearchmodule

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.os.UserHandle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppGridItemBinding
import kenneth.app.starlightlauncher.appsearchmodule.view.AppOptionMenu
import kotlin.math.min

/**
 * A grid adapter for [RecyclerView] to show [apps] in a grid of icons.
 */
internal class AppGridAdapter(
    private val context: Context,
    apps: AppList,
    private val launcher: StarlightLauncherApi,
    /**
     * Whether app names should be shown underneath each app icon.
     */
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
    /**
     * Whether the grid is in dnd mode.
     */
    var isDraggingAndDropping = false

    private val apps = apps.toMutableList()

    private val visibleApps = mutableListOf<LauncherActivityInfo>()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appSearchModulePreferences = AppSearchModulePreferences.getInstance(context)
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val inputMethodManager = context.getSystemService<InputMethodManager>()

    private lateinit var selectedApp: LauncherActivityInfo
    private var recyclerView: RecyclerView? = null
    private var gridSpanCount = 0

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            packageName?.let { removeAppFromGrid(it) }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null) return
            launcherApps.getActivityList(packageName, user).forEach { launcherActivityInfo ->
                this@AppGridAdapter.apps.removeAll {
                    it.applicationInfo.packageName == packageName &&
                            it.componentName == launcherActivityInfo.componentName
                }
                val i = visibleApps.indexOfFirst {
                    it.applicationInfo.packageName == packageName &&
                            it.componentName == launcherActivityInfo.componentName
                }
                if (i >= 0) {
                    visibleApps[i] = launcherActivityInfo
                    notifyItemChanged(i)
                }
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            packageNames?.forEach { packageName ->
                removeAppFromGrid(packageName)
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {}
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        launcherApps.registerCallback(launcherAppsCallback)
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
        launcherApps.unregisterCallback(launcherAppsCallback)
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

                Glide.with(context)
                    .load(launcher.getIconPack().getIconOf(app, app.user))
                    .into(this)
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

    fun refresh() {
        notifyItemRangeChanged(0, visibleApps.size)
    }

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

    private fun showAppOptionMenu(): Boolean {
        recyclerView?.let {
            inputMethodManager?.hideSoftInputFromWindow(it.windowToken, 0)
        }
        launcher.showOptionMenu { menu -> AppOptionMenu(context, selectedApp, menu) }
        return true
    }

    private fun openSelectedApp(sourceIconView: View) {
        if (!isDraggingAndDropping) {
            val sourceBounds = Rect().run {
                sourceIconView.getGlobalVisibleRect(this)
                this
            }

            context.getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(sourceIconView.windowToken, 0)

            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    component = selectedApp.componentName
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            )
        }
    }

    private fun removeAppFromGrid(packageName: String) {
        this@AppGridAdapter.apps.removeAll { it.applicationInfo.packageName == packageName }
        val i = visibleApps.indexOfFirst { it.applicationInfo.packageName == packageName }
        if (i >= 0) {
            visibleApps.removeAt(i)
            notifyItemRemoved(i)
            if (hasMore()) {
                val lastIndex = visibleApps.size
                visibleApps += apps[lastIndex]
                notifyItemInserted(lastIndex)
            }
        }
    }
}

internal class AppGridItem(internal val binding: AppGridItemBinding) :
    RecyclerView.ViewHolder(binding.root)
