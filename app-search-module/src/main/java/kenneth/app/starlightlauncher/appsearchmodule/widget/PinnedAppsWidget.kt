package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.appsearchmodule.AppGridAdapter
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferenceChanged
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding

internal class PinnedAppsWidget(
    private val binding: PinnedAppsWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder {
    override val rootView: View = binding.root

    private val context = rootView.context
    private val prefs = AppSearchModulePreferences.getInstance(context)
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private var appGridAdapter: AppGridAdapter? = null

    private var dndTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        0,
    ) {
        private var hasItemMoved = false

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder?.itemView
                    ?.animate()
                    ?.scaleX(1.1f)
                    ?.scaleY(1.1f)
                    ?.setDuration(200)
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView
                .animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)

            if (!hasItemMoved) {
                appGridAdapter?.showAppOptionMenuAtPosition(viewHolder.adapterPosition)
            } else {
                hasItemMoved = false
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            prefs.swapPinnedApps(viewHolder.adapterPosition, target.adapterPosition)
            recyclerView.adapter?.notifyItemMoved(from, to)
            return true
        }

        override fun onMoved(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            fromPos: Int,
            target: RecyclerView.ViewHolder,
            toPos: Int,
            x: Int,
            y: Int
        ) {
            super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
            hasItemMoved = true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    })

    init {
        prefs.addOnPreferenceChangedListener(::onPreferenceChanged)

        if (prefs.hasPinnedApps) {
            showWidget()
        } else {
            hideWidget()
        }

        dndTouchHelper.attachToRecyclerView(binding.pinnedAppsGrid)
    }

    private fun onPreferenceChanged(event: AppSearchModulePreferenceChanged) {
        when (event) {
            is AppSearchModulePreferenceChanged.PinnedAppAdded -> {
                if (!rootView.isVisible) {
                    showWidget()
                } else {
                    appGridAdapter?.addAppToGrid(event.app)
                }
            }

            is AppSearchModulePreferenceChanged.PinnedAppRemoved -> {
                if (prefs.hasPinnedApps) {
                    appGridAdapter?.removeAppFromGrid(event.position)
                } else {
                    // no more pinned apps, hide the widget
                    hideWidget()
                }
            }

            is AppSearchModulePreferenceChanged.PinnedAppLabelVisibilityChanged -> {
                if (event.isVisible) {
                    appGridAdapter?.showAppLabels()
                } else {
                    appGridAdapter?.hideAppLabels()
                }
            }

            else -> {}
        }
    }

    private fun showWidget() {
        with(binding) {
            rootView.isVisible = true

            val pinnedApps = prefs.pinnedApps.mapNotNull { pinnedAppName ->
                launcherApps.getActivityList(pinnedAppName.packageName, Process.myUserHandle())
                    .find { it.componentName == pinnedAppName }
            }

            pinnedAppsGrid.apply {
                layoutManager = GridLayoutManager(context, 5)
                adapter =
                    AppGridAdapter(
                        context,
                        pinnedApps,
                        launcher,
                        prefs.shouldShowPinnedAppNames,
                        showAllApps = true,
                        enableLongPressMenu = false,
                    )
                        .also { appGridAdapter = it }
            }

            if (prefs.shouldShowPinnedAppNames) {
                appGridAdapter?.showAppLabels()
            } else {
                appGridAdapter?.hideAppLabels()
            }

            pinnedAppsWidget.blurWith(launcher.blurHandler)
        }
    }

    private fun hideWidget() {
        rootView.isVisible = false
        appGridAdapter = null
    }
}
