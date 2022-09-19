package kenneth.app.starlightlauncher.appsearchmodule

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import androidx.datastore.preferences.core.edit
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections.swap

/**
 * Manages preferences of [AppSearchModule]
 */
internal class AppSearchModulePreferences
private constructor(launcher: StarlightLauncherApi) {
    companion object {
        private var instance: AppSearchModulePreferences? = null

        fun getInstance(launcher: StarlightLauncherApi) =
            instance ?: AppSearchModulePreferences(launcher)
                .also { instance = it }
    }

    private val dataStore = launcher.dataStore

    val pinnedApps = dataStore.data
        .map { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let { json ->
                Json.decodeFromString<List<String>>(json)
                    .mapNotNull { ComponentName.unflattenFromString(it) }
            } ?: emptyList()
        }.shareIn(
            scope = launcher.coroutineScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed()
        )

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_app_names"`
     */
    val shouldShowAppNames = dataStore.data
        .map {
            it[PREF_KEY_SHOW_APP_NAMES]
                ?: launcher.context.resources.getBoolean(R.bool.def_pref_show_app_names)
        }

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_pinned_app_names"`
     */
    val shouldShowPinnedAppNames = dataStore.data
        .map {
            it[PREF_KEY_SHOW_PINNED_APP_NAMES]
                ?: launcher.context.resources.getBoolean(R.bool.def_pref_show_pinned_app_names)
        }

    fun isAppPinned(app: LauncherActivityInfo) =
        pinnedApps.map { apps -> apps.find { it == app.componentName } != null }

    suspend fun addPinnedApp(app: LauncherActivityInfo) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let {
                preferences[PREF_KEY_PINNED_APPS] = Json.encodeToString(
                    Json.decodeFromString<List<String>>(it) + app.componentName.flattenToString()
                )
            } ?: kotlin.run {
                preferences[PREF_KEY_PINNED_APPS] = Json.encodeToString(
                    listOf(app.componentName.flattenToString())
                )
            }

        }
    }

    suspend fun removePinnedApp(app: LauncherActivityInfo) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let { json ->
                preferences[PREF_KEY_PINNED_APPS] = Json.encodeToString(
                    Json.decodeFromString<List<String>>(json).filter {
                        app.componentName.flattenToString() != it
                    }
                )
            }

        }
    }

    suspend fun swapPinnedApps(fromPosition: Int, toPosition: Int) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let { json ->
                preferences[PREF_KEY_PINNED_APPS] = Json.encodeToString(
                    Json.decodeFromString<List<String>>(json).apply {
                        swap(this, fromPosition, toPosition)
                    }
                )
            }
        }
    }
}
