package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.AppOptionMenu
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppGridItemBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.min

/**
 * A grid adapter for [RecyclerView] to show [apps] in a grid of icons.
 */
internal class AppGridAdapter(
    private val context: Context,
    apps: AppList,
    private val launcher: StarlightLauncherApi,
    private var iconPack: IconPack,
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
) : RecyclerView.Adapter<AppGridItem>() {
    /**
     * Whether the grid is in dnd mode.
     */
    var isDraggingAndDropping = false

    private val apps = apps.toMutableList()
    private val visibleApps = mutableListOf<LauncherActivityInfo>()

    private val inputMethodManager = context.getSystemService<InputMethodManager>()
    private val prefs = AppSearchModulePreferences.getInstance(launcher.dataStore)

    private var recyclerView: RecyclerView? = null
    private var gridSpanCount = 0

    private val appOptionMenuCallback = object : AppOptionMenu.ActionCallback {
        override fun onUninstallApp(app: LauncherActivityInfo) {
            context.startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    Uri.fromParts("package", app.applicationInfo.packageName, null)
                )
            )
        }

        override fun onPinApp(app: LauncherActivityInfo) {
            launcher.coroutineScope.launch {
                prefs.addPinnedApp(app)
            }
        }

        override fun onUnpinApp(app: LauncherActivityInfo) {
            launcher.coroutineScope.launch {
                prefs.removePinnedApp(app)
            }
        }

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        override fun onOpenShortcut(shortcut: ShortcutInfo, sourceBound: Rect) {
            (context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps).startShortcut(
                shortcut,
                sourceBound,
                null
            )
        }
    }

    init {
        launcher.coroutineScope.run {
            launch { listenToLauncherEvents() }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
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
                    .load(iconPack.getIconOf(app, app.user))
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
                        showAppOptionMenu(app)
                    }
                }

                setOnClickListener {
                    openSelectedApp(app, holder.binding.appIcon)
                }
            }
        }
    }

    override fun getItemCount(): Int = visibleApps.size

    fun refresh() {
        notifyItemRangeChanged(0, visibleApps.size)
    }

    fun update(newApps: AppList) {
        val diffResult = DiffUtil.calculateDiff(
            AppListDiffCallback(apps, newApps)
        )

        apps.apply {
            clear()
            addAll(newApps)
        }
        visibleApps.apply {
            clear()
            addAll(
                if (showAllApps) newApps
                else newApps.subList(0, min(newApps.size, gridSpanCount))
            )
        }

        diffResult.dispatchUpdatesTo(this)
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
            showAppOptionMenu(apps[position])
        }
    }

    fun changeIconPack(iconPack: IconPack) {
        this.iconPack = iconPack
        notifyItemRangeChanged(0, visibleApps.size)
    }

    private suspend fun listenToLauncherEvents() {
        launcher.addLauncherEventListener {
            when (it) {
                is LauncherEvent.AppRemoved -> {
                    removeAppFromGrid(it.packageName)
                }

                is LauncherEvent.AppsChanged -> {
                    it.apps.forEach { app ->
                        changeAppInGrid(app)
                    }
                }
            }
        }
    }

    private fun changeAppInGrid(launcherActivityInfo: LauncherActivityInfo) {
        val packageName = launcherActivityInfo.applicationInfo.packageName
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

    private fun showAppOptionMenu(app: LauncherActivityInfo): Boolean {
        recyclerView?.let {
            inputMethodManager?.hideSoftInputFromWindow(it.windowToken, 0)
        }
        launcher.showOptionMenu { menu ->
            AppOptionMenu(
                context,
                app,
                iconPack = runBlocking { launcher.iconPack.first() },
                isAppPinned = runBlocking { prefs.isAppPinned(app).first() },
                menu,
                appOptionMenuCallback,
            )
        }
        return true
    }

    private fun openSelectedApp(app: LauncherActivityInfo, sourceIconView: View) {
        if (!isDraggingAndDropping) {
            context.getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(sourceIconView.windowToken, 0)

            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    component = app.componentName
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
