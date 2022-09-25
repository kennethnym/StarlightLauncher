package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.appsearchmodule.*
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class PinnedAppsWidget(
    private val binding: PinnedAppsWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder {
    override val rootView: View = binding.root

    private val prefs = AppSearchModulePreferences.getInstance(launcher)
    private var appGridAdapter: AppGridAdapter? = null

    private var dndTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        0,
    ) {
        private var hasItemMoved = false

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                appGridAdapter?.isDraggingAndDropping = true
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

            appGridAdapter?.isDraggingAndDropping = false
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
            launcher.coroutineScope.launch {
                prefs.swapPinnedApps(viewHolder.adapterPosition, target.adapterPosition)
            }
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
        launcher.coroutineScope.run {
            launch { launcher.addLauncherEventListener(::onLauncherEvent) }
            launch { listenToPinnedApps() }
            launch { listenToIconPack() }
            launch { updatePinnedAppLabelsVisibility() }
        }

        dndTouchHelper.attachToRecyclerView(binding.pinnedAppsGrid)
    }

    private suspend fun listenToIconPack() {
        launcher.iconPack.collect {
            appGridAdapter?.changeIconPack(it)
        }
    }

    private suspend fun listenToPinnedApps() {
        prefs.pinnedApps
            .map {
                it.mapNotNull { componentName -> launcher.launcherActivityInfoOf(componentName) }
            }
            .collect { pinnedApps ->
                when {
                    pinnedApps.isEmpty() -> {
                        hideWidget()
                    }
                    !rootView.isVisible -> {
                        showWidget(pinnedApps)
                    }
                    else -> {
                        appGridAdapter?.update(pinnedApps)
                    }
                }
            }
    }

    private suspend fun updatePinnedAppLabelsVisibility() {
        prefs.shouldShowPinnedAppNames.collect { shouldShowPinnedAppNames ->
            if (shouldShowPinnedAppNames) {
                appGridAdapter?.showAppLabels()
            } else {
                appGridAdapter?.hideAppLabels()
            }
        }
    }

    private fun onLauncherEvent(event: LauncherEvent) {
        when (event) {
            is LauncherEvent.IconPackChanged -> {
                appGridAdapter?.refresh()
            }
        }
    }

    private fun showWidget(apps: AppList) {
        with(binding) {
            rootView.isVisible = true

            pinnedAppsGrid.apply {
                layoutManager = GridLayoutManager(context, 5)
                adapter =
                    AppGridAdapter(
                        context,
                        apps,
                        launcher,
                        iconPack = runBlocking { launcher.iconPack.first() },
                        shouldShowAppNames = runBlocking { prefs.shouldShowAppNames.first() },
                        showAllApps = true,
                        enableLongPressMenu = false,
                    )
                        .also { appGridAdapter = it }
            }

            pinnedAppsWidget.blurWith(launcher.blurHandler)
        }
    }

    private fun hideWidget() {
        rootView.isVisible = false
        appGridAdapter = null
    }
}
