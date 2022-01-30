package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

private const val DEFAULT_SHOW_APP_LABELS = true

internal class AppSearchModulePreferences(
    private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val keys = PrefKeys(context)

    var shouldShowAppLabels = prefs.getBoolean(keys.showAppLabels, DEFAULT_SHOW_APP_LABELS)
        private set

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            keys.showAppLabels -> {
                shouldShowAppLabels = sharedPreferences.getBoolean(key, DEFAULT_SHOW_APP_LABELS)
            }
        }
    }
}

internal class PrefKeys(context: Context) {
    val showAppLabels by lazy {
        context.getString(R.string.pref_key_show_app_names)
    }
}