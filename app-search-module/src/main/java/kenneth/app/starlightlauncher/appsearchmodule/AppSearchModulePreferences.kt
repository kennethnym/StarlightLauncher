package kenneth.app.starlightlauncher.appsearchmodule

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Manages preferences of [AppSearchModule]
 */
internal class AppSearchModulePreferences
private constructor(private val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AppSearchModulePreferences? = null

        fun getInstance(context: Context) =
            instance ?: AppSearchModulePreferences(context.applicationContext)
                .also { instance = it }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Stores preferences keys used by these preferences.
     */
    val keys = PrefKeys(context)

    /**
     * Whether app labels should be visible.
     *
     * Key: `"pref_key_show_app_names"`
     */
    var shouldShowAppLabels = prefs.getBoolean(
        keys.showAppLabels,
        context.resources.getBoolean(R.bool.def_pref_show_app_names)
    )
        private set

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            keys.showAppLabels -> {
                shouldShowAppLabels = sharedPreferences.getBoolean(
                    key,
                    context.resources.getBoolean(R.bool.def_pref_show_app_names)
                )
            }
        }
    }
}

internal class PrefKeys(context: Context) {
    /**
     * Key: `"pref_key_show_app_names"`
     */
    val showAppLabels by lazy {
        context.getString(R.string.pref_key_show_app_names)
    }
}