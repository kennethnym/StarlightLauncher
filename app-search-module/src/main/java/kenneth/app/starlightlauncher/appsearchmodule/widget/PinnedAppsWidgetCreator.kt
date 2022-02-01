package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding

class PinnedAppsWidgetCreator : WidgetCreator {
    override fun createWidget(parent: ViewGroup, launcher: StarlightLauncherApi): WidgetHolder {
        val binding =
            PinnedAppsWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PinnedAppsWidget(binding, launcher)
    }
}