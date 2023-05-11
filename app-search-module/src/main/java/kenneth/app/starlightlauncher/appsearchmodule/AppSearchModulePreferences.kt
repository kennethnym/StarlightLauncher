package kenneth.app.starlightlauncher.appsearchmodule

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections.swap

const val DEFAULT_SHOW_APP_NAMES = true
const val DEFAULT_SHOW_PINNED_APP_NAMES = true

/**
 * Manages preferences of [AppSearchModule]
 */
class AppSearchModulePreferences
private constructor(private val dataStore: DataStore<Preferences>) {
    companion object {
        private var instance: AppSearchModulePreferences? = null

        fun getInstance(dataStore: DataStore<Preferences>) =
            instance ?: AppSearchModulePreferences(dataStore)
                .also { instance = it }
    }

    val pinnedApps = dataStore.data
        .map { preferences ->
            preferences[PREF_KEY_PINNED_APPS]?.let { json ->
                Json.decodeFromString<List<String>>(json)
                    .mapNotNull { ComponentName.unflattenFromString(it) }
            } ?: emptyList()
        }

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_app_names"`
     */
    val shouldShowAppNames = dataStore.data
        .map {
            it[PREF_KEY_SHOW_APP_NAMES] ?: DEFAULT_SHOW_APP_NAMES
        }
        .distinctUntilChanged()

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_pinned_app_names"`
     */
    val shouldShowPinnedAppNames = dataStore.data
        .map {
            it[PREF_KEY_SHOW_PINNED_APP_NAMES] ?: DEFAULT_SHOW_PINNED_APP_NAMES
        }
        .distinctUntilChanged()

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

    suspend fun setAppNamesVisibility(isVisible: Boolean) {
        dataStore.edit {
            it[PREF_KEY_SHOW_APP_NAMES] = isVisible
        }
    }

    suspend fun setPinnedAppNamesVisibility(isVisible: Boolean) {
        dataStore.edit {
            it[PREF_KEY_SHOW_PINNED_APP_NAMES] = isVisible
        }
    }
}
