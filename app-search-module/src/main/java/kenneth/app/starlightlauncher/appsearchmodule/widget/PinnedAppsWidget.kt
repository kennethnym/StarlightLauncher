package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.content.SharedPreferences
import android.view.View
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.appsearchmodule.AppGridAdapter
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding

internal class PinnedAppsWidget(
    private val binding: PinnedAppsWidgetBinding,
    private val launcher: StarlightLauncherApi
) : WidgetHolder,
    SharedPreferences.OnSharedPreferenceChangeListener {
    override val rootView: View = binding.root

    private val context = rootView.context
    private val prefs = AppSearchModulePreferences.getInstance(context)
    private var appGridAdapter: AppGridAdapter? = null

    init {
        PreferenceManager.getDefaultSharedPreferences(context)
            .registerOnSharedPreferenceChangeListener(this)

        if (prefs.hasPinnedApps) {
            showWidget()
        } else {
            hideWidget()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when {
            // pinned apps are stored as individual pref keys
            // with prefs.keys.pinnedApps as prefix and their indices as the suffix
            key?.matches(Regex("^${prefs.keys.pinnedApps}\\d")) == true -> {
                when {
                    sharedPreferences?.contains(key) == true -> {
                        if (!rootView.isVisible) {
                            showWidget()
                        } else {
                            key.removePrefix(prefs.keys.pinnedApps).toIntOrNull()
                                ?.let { index ->
                                    appGridAdapter?.addAppToGrid(prefs.pinnedApps[index], index)
                                }
                        }
                    }

                    prefs.hasPinnedApps -> {
                        key.removePrefix(prefs.keys.pinnedApps).toIntOrNull()
                            ?.let { index ->
                                appGridAdapter?.removeAppFromGrid(index)
                            }
                    }

                    else -> hideWidget()
                }
            }
        }
    }

    private fun showWidget() {
        with(binding) {
            rootView.isVisible = true

            pinnedAppsGrid.apply {
                adapter =
                    AppGridAdapter(context, prefs.pinnedApps, launcher)
                        .also { appGridAdapter = it }
                layoutManager = GridLayoutManager(context, 5)
            }

            pinnedAppsWidget.blurWith(launcher.blurHandler)
        }
    }

    private fun hideWidget() {
        rootView.isVisible = false
        appGridAdapter = null
    }
}