package kenneth.app.starlightlauncher.appsearchmodule

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.api.util.EventChannel
import kenneth.app.starlightlauncher.api.util.swap
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class AppSearchModulePreferenceChanged {
    /**
     * A pinned app is added by the user.
     */
    data class PinnedAppAdded(val app: LauncherActivityInfo) : AppSearchModulePreferenceChanged()

    /**
     * A pinned app is removed by the user.
     */
    data class PinnedAppRemoved(val app: LauncherActivityInfo, val position: Int) :
        AppSearchModulePreferenceChanged()

    /**
     * User has changed the visibility of labels of apps in search result.
     */
    data class AppLabelVisibilityChanged(val isVisible: Boolean) :
        AppSearchModulePreferenceChanged()

    /**
     * User has changed the visibility of labels of pinned apps.
     */
    data class PinnedAppLabelVisibilityChanged(val isVisible: Boolean) :
        AppSearchModulePreferenceChanged()
}

/**
 * Manages preferences of [AppSearchModule]
 */
internal class AppSearchModulePreferences
private constructor(private val context: Context) :
    EventChannel<AppSearchModulePreferenceChanged>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private var instance: AppSearchModulePreferences? = null

        fun getInstance(context: Context) =
            instance ?: AppSearchModulePreferences(context.applicationContext)
                .also { instance = it }
    }

    /**
     * Stores preferences keys used by these preferences.
     */
    val keys = PrefKeys(context)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _pinnedApps = mutableListOf<ComponentName>()

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_app_names"`
     */
    var shouldShowAppNames = sharedPreferences.getBoolean(
        keys.showAppNames,
        context.resources.getBoolean(R.bool.def_pref_show_app_names)
    )
        private set

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_pinned_app_names"`
     */
    var shouldShowPinnedAppNames = sharedPreferences.getBoolean(
        keys.showPinnedAppNames,
        context.resources.getBoolean(R.bool.def_pref_show_pinned_app_names)
    )
        private set

    val pinnedApps
        get() = _pinnedApps.toList()

    val hasPinnedApps
        get() = pinnedApps.isNotEmpty()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        loadPinnedApps()
    }

    fun isAppPinned(app: LauncherActivityInfo) =
        _pinnedApps.find { it == app.componentName } != null

    fun addPinnedApp(app: LauncherActivityInfo) {
        _pinnedApps += app.componentName
        savePinnedApps()
        add(AppSearchModulePreferenceChanged.PinnedAppAdded(app))
    }

    fun removePinnedApp(app: LauncherActivityInfo) {
        val position = _pinnedApps.indexOfFirst { it == app.componentName }
        _pinnedApps.removeAt(position)
        savePinnedApps()
        add(AppSearchModulePreferenceChanged.PinnedAppRemoved(app, position))
    }

    fun swapPinnedApps(fromPosition: Int, toPosition: Int) {
        _pinnedApps.swap(fromPosition, toPosition)
        savePinnedApps()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return

        when (key) {
            keys.showAppNames -> {
                shouldShowAppNames = sharedPreferences.getBoolean(
                    key,
                    context.resources.getBoolean(R.bool.def_pref_show_app_names)
                ).also {
                    add(AppSearchModulePreferenceChanged.AppLabelVisibilityChanged(it))
                }
            }
            keys.showPinnedAppNames -> {
                shouldShowPinnedAppNames = sharedPreferences.getBoolean(
                    key,
                    context.resources.getBoolean(R.bool.def_pref_show_pinned_app_names)
                ).also {
                    add(AppSearchModulePreferenceChanged.PinnedAppLabelVisibilityChanged(it))
                }
            }
        }
    }

    /**
     * Loads pinned apps from storage and checks if they are still installed.
     * If not, update the list.
     */
    private fun loadPinnedApps() {
        var needsUpdate = false

        sharedPreferences.getString(keys.pinnedApps, null)
            ?.let { Json.decodeFromString<List<String>>(it) }
            ?.forEach {
                needsUpdate = ComponentName.unflattenFromString(it)
                    ?.let { componentName ->
                        try {
                            context.packageManager.getPackageInfo(componentName.packageName, 0)
                            _pinnedApps += componentName
                            false
                        } catch (ex: PackageManager.NameNotFoundException) {
                            true
                        }
                    }
                    ?: true
            }

        if (needsUpdate) savePinnedApps()
    }

    private fun savePinnedApps() {
        sharedPreferences.edit(commit = true) {
            putString(
                keys.pinnedApps,
                Json.encodeToString(_pinnedApps.map { it.flattenToString() })
            )
        }
    }
}

internal class PrefKeys(context: Context) {
    /**
     * Key: `"pref_key_show_app_names"`
     */
    val showAppNames by lazy {
        context.getString(R.string.pref_key_show_app_names)
    }

    /**
     * Key: `"pref_key_show_pinned_app_names"`
     */
    val showPinnedAppNames by lazy {
        context.getString(R.string.pref_key_show_pinned_app_names)
    }

    /**
     * Key: `"pref_key_pinned_apps"`
     */
    val pinnedApps by lazy {
        context.getString(R.string.pref_key_pinned_apps)
    }

    val pinnedAppsCount by lazy {
        context.getString(R.string.pref_key_pinned_apps_count)
    }
}