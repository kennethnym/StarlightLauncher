package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import kenneth.app.starlightlauncher.utils.ComponentNameSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class AddedWidget(val id: Int) {
    @Serializable
    data class StarlightWidget(
        val internalId: Int,
        val extensionName: String
    ) : AddedWidget(internalId)

    @Serializable
    data class AndroidWidget(
        val internalId: Int,
        @Serializable(with = ComponentNameSerializer::class)
        val provider: ComponentName,
        val appWidgetId: Int,
        val height: Int,
    ) : AddedWidget(internalId)
}