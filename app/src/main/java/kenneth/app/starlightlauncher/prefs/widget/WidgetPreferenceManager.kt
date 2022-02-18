package kenneth.app.starlightlauncher.prefs.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDGET_ORDER_LIST_SEPARATOR = ";"

sealed class WidgetPreferenceChanged {
    data class WidgetOrderChanged(
        val fromPosition: Int,
        val toPosition: Int,
    ) : WidgetPreferenceChanged()
}

typealias WidgetPreferenceListener = (event: WidgetPreferenceChanged) -> Unit

@Singleton
class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val extensionManager: ExtensionManager,
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
}
