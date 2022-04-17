package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
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

    init {
        prefs.addOnPreferenceChangedListener(::onPreferenceChanged)

        if (prefs.hasPinnedApps) {
            showWidget()
        } else {
            hideWidget()
        }
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