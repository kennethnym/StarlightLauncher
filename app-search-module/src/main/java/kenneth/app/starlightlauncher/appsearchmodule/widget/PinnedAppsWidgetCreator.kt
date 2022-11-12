package kenneth.app.starlightlauncher.appsearchmodule.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.appsearchmodule.databinding.PinnedAppsWidgetBinding

class PinnedAppsWidgetCreator(context: Context) : WidgetCreator(context) {
    private lateinit var launcher: StarlightLauncherApi

    override val metadata = Metadata(
        extensionName = context.getString(R.string.app_search_module_name),
        displayName = context.getString(R.string.app_search_module_display_name),
        description = context.getString(R.string.app_search_module_description),
    )

    override fun initialize(launcher: StarlightLauncherApi) {
        this.launcher = launcher
    }

    override fun createWidget(parent: ViewGroup): WidgetHolder {
        val binding =
            PinnedAppsWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PinnedAppsWidget(binding, launcher)
    }
}