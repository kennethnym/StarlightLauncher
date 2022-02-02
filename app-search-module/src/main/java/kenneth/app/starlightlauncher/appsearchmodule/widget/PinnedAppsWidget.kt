package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.api.preference.ObservablePreferencesListener
import kenneth.app.starlightlauncher.appsearchmodule.AppGridAdapter
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding
import java.util.*

internal class PinnedAppsWidget(
    private val binding: PinnedAppsWidgetBinding,
    private val launcher: StarlightLauncherApi
) : WidgetHolder {
    override val rootView: View = binding.root

    private val context = rootView.context
    private val prefs = AppSearchModulePreferences.getInstance(context)
    private var appGridAdapter: AppGridAdapter? = null

    init {
        prefs.addPreferencesListener(::onPreferencesChanged)

        if (prefs.hasPinnedApps) {
            showWidget()
        } else {
            hideWidget()
        }
    }

    private fun onPreferencesChanged(preferences: AppSearchModulePreferences, key: String) {
        when {
            // pinned apps are stored as individual pref keys
            // with prefs.keys.pinnedApps as prefix and their indices as the suffix
            key.matches(Regex("^${preferences.keys.pinnedApps}\\d")) -> {
                when {
                    preferences.sharedPreferences.contains(key) -> {
                        if (!rootView.isVisible) {
                            showWidget()
                        } else {
                            key.removePrefix(preferences.keys.pinnedApps).toIntOrNull()
                                ?.let { index ->
                                    appGridAdapter?.addAppToGrid(
                                        preferences.pinnedApps[index],
                                        index
                                    )
                                }
                        }
                    }

                    preferences.hasPinnedApps -> {
                        key.removePrefix(preferences.keys.pinnedApps).toIntOrNull()
                            ?.let { index ->
                                appGridAdapter?.removeAppFromGrid(index)
                            }
                    }

                    else -> hideWidget()
                }
            }

            key == preferences.keys.showPinnedAppNames -> {
                if (preferences.shouldShowPinnedAppNames) {
                    appGridAdapter?.showAppLabels()
                } else {
                    appGridAdapter?.hideAppLabels()
                }
            }
        }
    }

    private fun showWidget() {
        with(binding) {
            rootView.isVisible = true

            pinnedAppsGrid.apply {
                adapter =
                    AppGridAdapter(
                        context,
                        prefs.pinnedApps,
                        launcher,
                        prefs.shouldShowPinnedAppNames
                    )
                        .also { appGridAdapter = it }
                layoutManager = GridLayoutManager(context, 5)
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