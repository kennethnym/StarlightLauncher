package kenneth.app.starlightlauncher.widgets

import android.content.ComponentName
import kenneth.app.starlightlauncher.util.ComponentNameSerializer
import kotlinx.serialization.Serializable

@Serializable
internal sealed class AddedWidget(val id: Int) {
    @Serializable
    data class StarlightWidget(
        val internalId: Int,
        val extensionName: String
    ) : AddedWidget(internalId)

    @Serializable
    data class AndroidWidget(
        @Serializable(with = ComponentNameSerializer::class)
        val provider: ComponentName,
        val appWidgetId: Int,
        val height: Int,
    ) : AddedWidget(appWidgetId)
}