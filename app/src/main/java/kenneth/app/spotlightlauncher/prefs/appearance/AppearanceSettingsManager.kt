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

    /**
     * The icon pack to use, if user has picked any. null if user has not picked any icon pack.
     */
    var iconPack: IconPack? = null
        private set

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    // === pref keys ===
    lateinit var showAppLabelsPrefKey: String
    lateinit var iconPackPrefKey: String

    fun getInstance(context: Context) = this.apply {
        this.context = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        initializePrefKeys()

        if (iconPack == null) {
            val selectedIconPackPackageName = sharedPreferences.getString(iconPackPrefKey, null)

            iconPack =
                if (selectedIconPackPackageName != null)
                    IconPack(context, selectedIconPackPackageName)
                else null
        }
    }

    /**
     * An alias to SharedPreferences.registerOnSharedPreferenceChangeListener
     */
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun changeIconPack(iconPack: IconPack) {
        sharedPreferences
            .edit()
            .putString(iconPackPrefKey, iconPack.packageName)
            .apply()

        this.iconPack = iconPack
    }

    /**
     * Revert the applied icon pack and use default icons instead.
     */
    fun useDefaultIconPack() {
        sharedPreferences
            .edit()
            .remove(iconPackPrefKey)
            .apply()

        iconPack = null
    }

    private fun initializePrefKeys() {
        if (!::showAppLabelsPrefKey.isInitialized) {
            showAppLabelsPrefKey = context.getString(R.string.appearance_show_app_labels)
        }

        if (!::iconPackPrefKey.isInitialized) {
            iconPackPrefKey = context.getString(R.string.appearance_icon_pack)
        }
    }
}