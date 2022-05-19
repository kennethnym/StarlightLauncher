package kenneth.app.starlightlauncher.unitconverterwidget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.unitconverterwidget.databinding.UnitConverterWidgetBinding

class UnitConverterWidgetCreator(context: Context) : WidgetCreator(context) {
    override val metadata = Metadata(
        extensionName = "kenneth.app.starlightlauncher.unitconverterwidget",
        displayName = context.getString(R.string.unit_converter_widget_display_name),
        description = context.getString(R.string.unit_converter_widget_description),
    )

    override fun createWidget(parent: ViewGroup, launcher: StarlightLauncherApi): WidgetHolder {
        val binding =
            UnitConverterWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnitConverterWidget(binding, launcher)
    }
}