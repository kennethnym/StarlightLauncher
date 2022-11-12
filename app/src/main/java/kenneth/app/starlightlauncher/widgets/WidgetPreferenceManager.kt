package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.*
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.appsearchmodule.PREF_KEY_PINNED_APPS
import kenneth.app.starlightlauncher.extension.ExtensionManager
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
    data class NewAndroidWidgetAdded(
        val addedWidget: AddedWidget.AndroidWidget,

        val appWidgetProviderInfo: AppWidgetProviderInfo,
    ) : WidgetPreferenceChanged()

    data class WidgetRemoved(
        val removedWidget: AddedWidget,
        val position: Int,
    ) : WidgetPreferenceChanged()
}

@Singleton
internal class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionManager: ExtensionManager,
    private val launcherEventChannel: LauncherEventChannel,
    private val random: Random,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private var _addedWidgets = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let {
                Json.decodeFromString<List<AddedWidget>>(it).toMutableList()
            } ?: mutableListOf<AddedWidget>().apply {
                extensionManager.installedExtensions.forEach { ext ->
                    if (ext.widget != null) add(
                        AddedWidget.StarlightWidget(
                            random.nextInt(),
                            ext.name,
                        )
                    )
                }
            }
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

    val addedWidgets: List<AddedWidget> = _addedWidgets

    fun isStarlightWidgetAdded(extensionName: String) =
        addedStarlightWidgets.contains(extensionName)

    suspend fun addStarlightWidget(extensionName: String): AddedWidget.StarlightWidget {
        val widgetId = random.nextInt()
        val newWidget = AddedWidget.StarlightWidget(
            internalId = widgetId,
            extensionName,
        )
        _addedWidgets += newWidget
        addedStarlightWidgets += extensionName

        saveAddedWidgets()

        return newWidget
    }

    suspend fun removeStarlightWidget(extensionName: String) {
        val widgetPos =
            _addedWidgets.indexOfFirst { it is AddedWidget.StarlightWidget && it.extensionName == extensionName }
        if (widgetPos < 0) return

        val removedWidget = _addedWidgets.removeAt(widgetPos)
        addedStarlightWidgets.remove((removedWidget as AddedWidget.StarlightWidget).extensionName)
        saveAddedWidgets()
        launcherEventChannel.add(
            WidgetPreferenceChanged.WidgetRemoved(
                removedWidget,
                widgetPos
            )
        )
    }

    suspend fun changeWidgetOrder(fromPosition: Int, toPosition: Int) {
        _addedWidgets.swap(fromPosition, toPosition)
        saveAddedWidgets()
    }

    suspend fun addAndroidWidget(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo) {
        val newWidget = AddedWidget.AndroidWidget(
            appWidgetProviderInfo.provider,
            appWidgetId,
            appWidgetProviderInfo.minHeight,
        )
        _addedWidgets += newWidget

        saveAddedWidgets()
        launcherEventChannel.add(
            WidgetPreferenceChanged.NewAndroidWidgetAdded(
                newWidget,
                appWidgetProviderInfo
            )
        )
    }

    /**
     * Change the height of [addedWidget]. [newHeight] must be specified in dp.
     */
    suspend fun changeWidgetHeight(addedWidget: AddedWidget, newHeight: Int) {
        if (addedWidget is AddedWidget.AndroidWidget) {
            val widgetPos = _addedWidgets.indexOfFirst { it.id == addedWidget.id }
            _addedWidgets[widgetPos] = addedWidget.copy(height = newHeight)
            saveAddedWidgets()
        }
    }

    suspend fun removeAndroidWidget(appWidgetId: Int) {
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val widgetPos =
            _addedWidgets.indexOfFirst { it is AddedWidget.AndroidWidget && it.provider == appWidgetProviderInfo.provider }
        if (widgetPos < 0) return

        val removedWidget = _addedWidgets.removeAt(widgetPos)

        saveAddedWidgets()
        launcherEventChannel.add(
            WidgetPreferenceChanged.WidgetRemoved(
                removedWidget,
                widgetPos
            )
        )
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
