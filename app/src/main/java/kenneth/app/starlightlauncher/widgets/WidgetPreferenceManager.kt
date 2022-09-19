package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.*
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.appsearchmodule.PREF_KEY_PINNED_APPS
import kenneth.app.starlightlauncher.prefs.PREF_ADDED_WIDGETS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal sealed class WidgetPreferenceChanged : InternalLauncherEvent() {
    data class WidgetOrderChanged(
        val fromPosition: Int,
        val toPosition: Int,
    ) : WidgetPreferenceChanged()

    data class NewAndroidWidgetAdded(
        val addedWidget: AddedWidget.AndroidWidget,

        val appWidgetProviderInfo: AppWidgetProviderInfo,
    ) : WidgetPreferenceChanged()

    data class NewStarlightWidgetAdded(
        val addedWidget: AddedWidget.StarlightWidget,
    ) : WidgetPreferenceChanged()

    data class WidgetRemoved(
        val removedWidget: AddedWidget,
        val position: Int,
    ) : WidgetPreferenceChanged()
}

@Singleton
internal class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,

    @Named(MAIN_DISPATCHER)
    private val dispatcher: CoroutineDispatcher,

    private val launcherEventChannel: LauncherEventChannel,
    private val random: Random,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private val coroutineScope = CoroutineScope(dispatcher)

    private var _addedWidgets = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let {
                Json.decodeFromString<List<AddedWidget>>(it).toMutableList()
            } ?: mutableListOf()
        }.first()
    }

    private var addedStarlightWidgets = mutableSetOf<String>().apply {
        addAll(
            _addedWidgets
                .asSequence()
                .filterIsInstance<AddedWidget.StarlightWidget>()
                .map { it.extensionName }
        )
    }

    val addedWidgets
        get() = _addedWidgets.toList()

    fun isStarlightWidgetAdded(extensionName: String) =
        addedStarlightWidgets.contains(extensionName)

    fun addStarlightWidget(extensionName: String) {
        val widgetId = random.nextInt()
        val newWidget = AddedWidget.StarlightWidget(
            internalId = widgetId,
            extensionName,
        )
        _addedWidgets += newWidget
        addedStarlightWidgets += extensionName
        coroutineScope.launch {
            saveAddedWidgets()
            launcherEventChannel.add(WidgetPreferenceChanged.NewStarlightWidgetAdded(newWidget))
        }
    }

    fun removeStarlightWidget(extensionName: String) {
        val widgetPos =
            _addedWidgets.indexOfFirst { it is AddedWidget.StarlightWidget && it.extensionName == extensionName }
        if (widgetPos < 0) return

        val removedWidget = _addedWidgets.removeAt(widgetPos)
        addedStarlightWidgets.remove((removedWidget as AddedWidget.StarlightWidget).extensionName)
        coroutineScope.launch {
            saveAddedWidgets()
            launcherEventChannel.add(
                WidgetPreferenceChanged.WidgetRemoved(
                    removedWidget,
                    widgetPos
                )
            )
        }
    }

    fun changeWidgetOrder(fromPosition: Int, toPosition: Int) {
        _addedWidgets.swap(fromPosition, toPosition)
        coroutineScope.launch {
            saveAddedWidgets()
            launcherEventChannel.add(
                WidgetPreferenceChanged.WidgetOrderChanged(
                    fromPosition,
                    toPosition
                )
            )
        }
    }

    fun addAndroidWidget(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo) {
        val newWidget = AddedWidget.AndroidWidget(
            appWidgetProviderInfo.provider,
            appWidgetId,
            appWidgetProviderInfo.minHeight,
        )
        _addedWidgets += newWidget

        coroutineScope.launch {
            saveAddedWidgets()
            launcherEventChannel.add(
                WidgetPreferenceChanged.NewAndroidWidgetAdded(
                    newWidget,
                    appWidgetProviderInfo
                )
            )
        }
    }

    /**
     * Change the height of [addedWidget]. [newHeight] must be specified in dp.
     */
    fun changeWidgetHeight(addedWidget: AddedWidget, newHeight: Int) {
        if (addedWidget is AddedWidget.AndroidWidget) {
            val widgetPos = _addedWidgets.indexOfFirst { it.id == addedWidget.id }
            _addedWidgets[widgetPos] = addedWidget.copy(height = newHeight)
            coroutineScope.launch { saveAddedWidgets() }
        }
    }

    fun removeAndroidWidget(appWidgetId: Int) {
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val widgetPos =
            _addedWidgets.indexOfFirst { it is AddedWidget.AndroidWidget && it.provider == appWidgetProviderInfo.provider }
        if (widgetPos < 0) return

        val removedWidget = _addedWidgets.removeAt(widgetPos)

        coroutineScope.launch {
            saveAddedWidgets()
            launcherEventChannel.add(
                WidgetPreferenceChanged.WidgetRemoved(
                    removedWidget,
                    widgetPos
                )
            )
        }
    }

    private suspend fun saveAddedWidgets() {
        context.dataStore.edit {
            it[PREF_ADDED_WIDGETS] = Json.encodeToString(_addedWidgets)
        }
    }
}

class WidgetPrefKeys(context: Context) {
    val widgetOrder by lazy { context.getString(R.string.pref_key_widget_order) }

    val addedWidgets by lazy { context.getString(R.string.pref_key_added_widgets) }
}
