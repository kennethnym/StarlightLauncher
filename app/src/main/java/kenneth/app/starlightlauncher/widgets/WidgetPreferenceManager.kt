package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.utils.ComponentNameSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDGET_ORDER_LIST_SEPARATOR = ";"
private const val ADDED_WIDGET_LIST_SEPARATOR = ";"

sealed class WidgetPreferenceChanged {
    data class WidgetOrderChanged(
        val fromPosition: Int,
        val toPosition: Int,
    ) : WidgetPreferenceChanged()

    data class NewAndroidWidgetAdded(
        val addedWidget: AddedWidget.AndroidWidget,
    ) : WidgetPreferenceChanged()
}

@Serializable
sealed class AddedWidget {
    @Serializable
    data class StarlightWidget(val extensionName: String) : AddedWidget()

    @Serializable
    data class AndroidWidget(
        @Serializable(with = ComponentNameSerializer::class)
        val provider: ComponentName,

        @Transient
        val appWidgetProviderInfo: AppWidgetProviderInfo? = null
    ) : AddedWidget()
}

typealias WidgetPreferenceListener = (event: WidgetPreferenceChanged) -> Unit

@Singleton
class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val extensionManager: ExtensionManager,
    private val appWidgetHost: AppWidgetHost
) : ObservablePreferences<WidgetPreferenceManager>(context) {
    val keys = WidgetPrefKeys(context)

    var widgetOrder =
        sharedPreferences.getString(keys.widgetOrder, null)
            ?.split(WIDGET_ORDER_LIST_SEPARATOR)
            ?: mutableSetOf<String>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.widget != null) add(ext.name)
                }
            }
        private set

    var addedWidgets =
        sharedPreferences.getString(keys.addedWidgets, null)
            ?.let { Json.decodeFromString<List<AddedWidget>>(it) }
            ?.toMutableList()
            ?: mutableListOf<AddedWidget>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.widget != null) add(AddedWidget.StarlightWidget(ext.name))
                }
            }

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    fun orderOf(extensionName: String) = widgetOrder.indexOf(extensionName)

    fun changeWidgetOrder(fromPosition: Int, toPosition: Int, newOrder: List<String>) {
        widgetOrder = newOrder
        sharedPreferences.edit(commit = true) {
            putString(
                keys.widgetOrder,
                newOrder.joinToString(WIDGET_ORDER_LIST_SEPARATOR)
            )
        }
        setChanged()
        notifyObservers(WidgetPreferenceChanged.WidgetOrderChanged(fromPosition, toPosition))
    }

    fun addAndroidWidget(info: AppWidgetProviderInfo) {
        val newWidget = AddedWidget.AndroidWidget(info.provider, info)
        addedWidgets += newWidget
        sharedPreferences.edit(commit = true) {
            putString(
                keys.addedWidgets,
                Json.encodeToString(addedWidgets)
            )
        }
        setChanged()
        notifyObservers(WidgetPreferenceChanged.NewAndroidWidgetAdded(newWidget))
    }

    fun addOnWidgetPreferenceChangedListener(listener: WidgetPreferenceListener) {
        addObserver { o, arg ->
            if (arg is WidgetPreferenceChanged) {
                listener(arg)
            }
        }
    }
}

class WidgetPrefKeys(context: Context) {
    val widgetOrder by lazy { context.getString(R.string.pref_key_widget_order) }

    val addedWidgets by lazy { context.getString(R.string.pref_key_added_widgets) }
}
