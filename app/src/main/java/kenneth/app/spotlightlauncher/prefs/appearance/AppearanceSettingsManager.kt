package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kenneth.app.spotlightlauncher.R

private object DefaultValue {
    const val SHOW_APP_LABELS = true
}

/**
 * An interface to interact with appearance preferences stored in SharedPreferences
 */
object AppearancePreferenceManager {
    /**
     * Whether the user chooses to show app labels under icons
     */
    val showAppLabels: Boolean
        get() = sharedPreferences.getBoolean(showAppLabelsPrefKey, DefaultValue.SHOW_APP_LABELS)

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    // === pref keys ===
    lateinit var showAppLabelsPrefKey: String

    fun getInstance(context: Context) = this.apply {
        this.context = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        initializePrefKeys()
    }

    /**
     * An alias to SharedPreferences.registerOnSharedPreferenceChangeListener
     */
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun initializePrefKeys() {
        if (!::showAppLabelsPrefKey.isInitialized) {
            showAppLabelsPrefKey = context.getString(R.string.appearance_show_app_labels)
        }
    }
}