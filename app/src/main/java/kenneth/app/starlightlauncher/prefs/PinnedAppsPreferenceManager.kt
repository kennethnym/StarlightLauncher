package kenneth.app.starlightlauncher.prefs

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.spotlightlauncher.R

object PinnedAppsPreferenceManager {
    lateinit var pinnedApps: MutableList<String>
        private set

    private const val APP_LIST_SEPARATOR = ";"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pinnedAppsPrefKey: String
    private lateinit var context: Context

    private var pinnedAppsListener: (() -> Unit)? = null

    fun getInstance(context: Context) = this.apply {
        PinnedAppsPreferenceManager.context = context

        if (!PinnedAppsPreferenceManager::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        }

        if (!PinnedAppsPreferenceManager::pinnedApps.isInitialized) {
            pinnedAppsPrefKey = context.getString(R.string.pinned_apps)
            pinnedApps = sharedPreferences
                .getString(pinnedAppsPrefKey, "")
                ?.split(APP_LIST_SEPARATOR)
                ?.toMutableList()
                ?: mutableListOf()
        }
    }

    fun addPinnedApps(app: ResolveInfo) {
        val appPackageName = app.activityInfo.packageName

        pinnedApps.add(appPackageName + app.loadLabel(context.packageManager))
        writeChanges()
        pinnedAppsListener?.let { it() }
    }

    fun removePinnedApps(app: ResolveInfo) {
        val appPackageName = app.activityInfo.packageName

        pinnedApps.remove(appPackageName)
        writeChanges()
        pinnedAppsListener?.let { it() }
    }

    fun setPinnedAppsListener(callback: () -> Unit) {
        pinnedAppsListener = callback
    }

    fun isAppPinned(app: ResolveInfo) =
        pinnedApps.contains(app.activityInfo.packageName + app.loadLabel(context.packageManager))

    private fun writeChanges() {
        with(sharedPreferences.edit()) {
            putString(pinnedAppsPrefKey, pinnedApps.joinToString(APP_LIST_SEPARATOR))
            apply()
        }
    }
}