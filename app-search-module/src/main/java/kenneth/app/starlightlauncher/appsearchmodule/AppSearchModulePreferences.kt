package kenneth.app.starlightlauncher.appsearchmodule

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences

/**
 * Manages preferences of [AppSearchModule]
 */
internal class AppSearchModulePreferences
private constructor(private val context: Context) :
    ObservablePreferences<AppSearchModulePreferences>(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AppSearchModulePreferences? = null

        fun getInstance(context: Context) =
            instance ?: AppSearchModulePreferences(context.applicationContext)
                .also { instance = it }
    }

    /**
     * Stores preferences keys used by these preferences.
     */
    val keys = PrefKeys(context)

    private var pinnedAppsCount = sharedPreferences.getInt(keys.pinnedAppsCount, 0)

    private val _pinnedApps = (0 until pinnedAppsCount)
        .mapIndexedNotNull { i, _ ->
            sharedPreferences.getString(keys.pinnedApps + i, null)
                ?.let {
                    try {
                        ComponentName.unflattenFromString(it)?.run {
                            context.packageManager.getActivityInfo(this, 0)
                        }
                    } catch (ex: PackageManager.NameNotFoundException) {
                        null
                    }
                }
        }
        .toMutableList()

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
    }

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            keys.showAppNames -> {
                shouldShowAppNames = sharedPreferences.getBoolean(
                    key,
                    context.resources.getBoolean(R.bool.def_pref_show_app_names)
                )
            }
            keys.showPinnedAppNames -> {
                shouldShowPinnedAppNames = sharedPreferences.getBoolean(
                    key,
                    context.resources.getBoolean(R.bool.def_pref_show_pinned_app_names)
                )
            }
        }
    }

    fun isAppPinned(app: ActivityInfo) =
        _pinnedApps.find { it.name == app.name } != null

    fun addPinnedApp(app: ActivityInfo) {
        _pinnedApps += app
        pinnedAppsCount += 1
        sharedPreferences.edit(commit = true) {
            val componentName = ComponentName.createRelative(app.packageName, app.name)
            putString(keys.pinnedApps + (pinnedAppsCount - 1), componentName.flattenToString())
            putInt(keys.pinnedAppsCount, pinnedAppsCount)
        }
    }

    fun removePinnedApp(app: ActivityInfo) {
        _pinnedApps.removeAt(
            _pinnedApps.indexOfFirst { it.name == app.name }
        )
        pinnedAppsCount -= 1
        sharedPreferences.edit(commit = true) {
            remove(keys.pinnedApps + pinnedAppsCount)
            putInt(keys.pinnedAppsCount, pinnedAppsCount)
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