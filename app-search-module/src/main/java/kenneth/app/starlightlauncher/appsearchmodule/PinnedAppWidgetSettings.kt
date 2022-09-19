package kenneth.app.starlightlauncher.appsearchmodule

import android.content.ComponentName
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PinnedAppWidgetSettings(private val launcher: StarlightLauncherApi) {
    private var pinnedApps: List<ComponentName> = listOf()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            launcher.dataStore.data.map { preferences ->
                preferences[PREF_KEY_PINNED_APPS]?.let { json ->
                    Json.decodeFromString<List<String>>(json)
                        .mapNotNull { ComponentName.unflattenFromString(it) }
                } ?: emptyList()
            }.collect {
                pinnedApps = it
            }
        }
    }

    fun isAppPinned(componentName: ComponentName) = pinnedApps.contains(componentName)
}