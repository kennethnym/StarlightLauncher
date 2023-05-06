package kenneth.app.starlightlauncher.widgets

import android.content.ComponentName
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.util.ComponentNameSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed class AddedWidget(val id: Int) {
    @Serializable
    data class StarlightWidget(
        val internalId: Int,
        val extensionName: String,
        @Transient
        val widgetCreator: WidgetCreator? = null,
    ) : AddedWidget(internalId)

    @Serializable
    data class AndroidWidget(
        @Serializable(with = ComponentNameSerializer::class)
        val provider: ComponentName,
        val appWidgetId: Int,
        val height: Int,
    ) : AddedWidget(appWidgetId)
}