package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.api.preference.PreferencesChanged
import kenneth.app.starlightlauncher.appsearchmodule.AppGridAdapter
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding

internal class PinnedAppsWidget(
    private val binding: PinnedAppsWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder {
    override val rootView: View = binding.root

    private val context = rootView.context
    private val prefs = AppSearchModulePreferences.getInstance(context)
    private var appGridAdapter: AppGridAdapter? = null

    init {
        with(prefs) {
            addOnPreferenceChangedListener(::onPreferencesChanged)
            addOnPreferenceRemovedListener(::onPreferencesRemoved)
        }

        if (prefs.hasPinnedApps) {
            showWidget()
        } else {
            hideWidget()
        }
    }

    private fun onPreferencesChanged(event: PreferencesChanged) {
        when {
            // pinned apps are stored as individual pref keys
            // with prefs.keys.pinnedApps as prefix and their indices as the suffix
            event.key.matches(Regex("^${prefs.keys.pinnedApps}\\d")) -> {
                if (!rootView.isVisible) {
                    showWidget()
                } else {
                    event.key.removePrefix(prefs.keys.pinnedApps).toIntOrNull()
                        ?.let { index ->
                            appGridAdapter?.addAppToGrid(
                                prefs.pinnedApps[index],
                                index
                            )
                        }
                }
            }

            event.key == prefs.keys.showPinnedAppNames -> {
                if (prefs.shouldShowPinnedAppNames) {
                    appGridAdapter?.showAppLabels()
                } else {
                    appGridAdapter?.hideAppLabels()
                }
            }
        }
    }

    private fun onPreferencesRemoved(event: PreferencesChanged) {
        if (event.key.matches(Regex("^${prefs.keys.pinnedApps}\\d"))) {
            if (prefs.hasPinnedApps) {
                event.key.removePrefix(prefs.keys.pinnedApps).toIntOrNull()
                    ?.let { index ->
                        appGridAdapter?.removeAppFromGrid(index)
                    }
            } else {
                hideWidget()
            }
        }
    }

    private fun showWidget() {
        with(binding) {
            rootView.isVisible = true

            pinnedAppsGrid.apply {
                layoutManager = GridLayoutManager(context, 5)
                adapter =
                    AppGridAdapter(
                        context,
                        prefs.pinnedApps,
                        launcher,
                        prefs.shouldShowPinnedAppNames
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