package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.InternalLauncherEvent
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.PREF_ADDED_WIDGETS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
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
        MutableStateFlow(
            context.dataStore.data.map { preferences ->
                preferences[PREF_ADDED_WIDGETS]?.let {
                    Json.decodeFromString<List<AddedWidget>>(it).mapNotNull { widget ->
                        if (widget is AddedWidget.StarlightWidget) {
                            extensionManager.lookupWidget(widget.extensionName)?.let { creator ->
                                widget.copy(widgetCreator = creator)
                            }
                        } else widget
                    }
                } ?: defaultWidgets()
            }.first()
        )
    }

    val addedWidgets: Flow<List<AddedWidget>> = _addedWidgets

    private var addedStarlightWidgets = mutableSetOf<String>().apply {
        addAll(
            _addedWidgets
                .value
                .asSequence()
                .filterIsInstance<AddedWidget.StarlightWidget>()
                .map { it.extensionName }
        )
    }

    fun isStarlightWidgetAdded(extensionName: String) =
        addedStarlightWidgets.contains(extensionName)

    suspend fun addStarlightWidget(extensionName: String): AddedWidget.StarlightWidget {
        val widgetId = random.nextInt()
        val newWidget = AddedWidget.StarlightWidget(
            internalId = widgetId,
            extensionName,
        )

        _addedWidgets.emit(_addedWidgets.value + newWidget)
        addedStarlightWidgets += extensionName

        saveAddedWidgets()

        return newWidget
    }

    suspend fun removeStarlightWidget(extensionName: String) {
        val currentAddedWidgets = _addedWidgets.value
        val newWidgetList =
            currentAddedWidgets.filterNot { it is AddedWidget.StarlightWidget && it.extensionName == extensionName }

        addedStarlightWidgets.remove(extensionName)
        saveAddedWidgets()

        _addedWidgets.emit(newWidgetList)
    }

    suspend fun changeWidgetOrder(fromPosition: Int, toPosition: Int) {
        _addedWidgets.value.toMutableList().run {
            swap(fromPosition, toPosition)
            _addedWidgets.emit(this)
        }
        saveAddedWidgets()
    }

    suspend fun addAndroidWidget(appWidgetId: Int, appWidgetProviderInfo: AppWidgetProviderInfo) {
        val newWidget = AddedWidget.AndroidWidget(
            appWidgetProviderInfo.provider,
            appWidgetId,
            appWidgetProviderInfo.minHeight,
        )
        _addedWidgets.emit(_addedWidgets.value + newWidget)

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
            val newWidgetList = _addedWidgets.value.map {
                if (it.id == addedWidget.id)
                    addedWidget.copy(height = newHeight)
                else
                    it
            }

            _addedWidgets.emit(newWidgetList)
            saveAddedWidgets()
        }
    }

    suspend fun removeAndroidWidget(appWidgetId: Int) {
        val newWidgetList =
            _addedWidgets.value.filterNot { it is AddedWidget.AndroidWidget && it.appWidgetId == appWidgetId }

        _addedWidgets.emit(newWidgetList)

        saveAddedWidgets()
    }

    private fun defaultWidgets() = mutableListOf<AddedWidget>().apply {
        extensionManager.installedExtensions.forEach { ext ->
            if (ext.widget != null) add(
                AddedWidget.StarlightWidget(
                    random.nextInt(),
                    ext.name,
                    ext.widget
                )
            )
        }
    }

    private suspend fun saveAddedWidgets() {
        context.dataStore.edit {
            it[PREF_ADDED_WIDGETS] = Json.encodeToString(_addedWidgets.value)
        }
    }
}

class WidgetPrefKeys(context: Context) {
    val widgetOrder by lazy { context.getString(R.string.pref_key_widget_order) }

    val addedWidgets by lazy { context.getString(R.string.pref_key_added_widgets) }
}
