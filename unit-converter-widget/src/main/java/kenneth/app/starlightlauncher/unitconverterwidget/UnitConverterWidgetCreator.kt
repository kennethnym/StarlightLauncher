package kenneth.app.starlightlauncher.unitconverterwidget

import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.unitconverterwidget.databinding.UnitConverterWidgetBinding

class UnitConverterWidgetCreator : WidgetCreator {
    override fun createWidget(parent: ViewGroup, launcher: StarlightLauncherApi): WidgetHolder {
        val binding =
            UnitConverterWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnitConverterWidget(binding, launcher)
    }
}