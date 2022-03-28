package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import kenneth.app.starlightlauncher.utils.ComponentNameSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class AddedWidget {
    @Serializable
    data class StarlightWidget(val extensionName: String) : AddedWidget()

    @Serializable
    data class AndroidWidget(
        @Serializable(with = ComponentNameSerializer::class)
        val provider: ComponentName,
    ) : AddedWidget()
}