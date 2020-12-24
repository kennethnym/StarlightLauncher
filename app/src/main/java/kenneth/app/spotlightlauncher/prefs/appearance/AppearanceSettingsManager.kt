package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kenneth.app.spotlightlauncher.R

private object DefaultValue {
    const val SHOW_APP_LABELS = true
    const val SHOW_PINNED_APPS_LABELS = true
    const val SHOW_APP_NAMES_IN_SEARCH_RESULT = true
}

/**
 * An interface to interact with appearance preferences stored in SharedPreferences
 */
object AppearancePreferenceManager {
    /**
     * Whether the user chooses to show names of pinned apps.
     */
    val showNamesOfPinnedApps: Boolean
        get() = sharedPreferences.getBoolean(
            showPinnedAppsLabelsKey,
            DefaultValue.SHOW_PINNED_APPS_LABELS
        )

    /**
     * Whether the user chooses to show names of apps in search result.
     */
    val showAppNamesInSearchResult: Boolean
        get() = sharedPreferences.getBoolean(
            showAppNamesInSearchResultKey,
            DefaultValue.SHOW_APP_NAMES_IN_SEARCH_RESULT,
        )

    /**
     * The icon pack to use, if user has picked any. null if user has not picked any icon pack.
     */
    var iconPack: IconPack? = null
        private set

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    lateinit var showPinnedAppsLabelsKey: String
    lateinit var showAppNamesInSearchResultKey: String
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
        if (!::iconPackPrefKey.isInitialized) {
            iconPackPrefKey = context.getString(R.string.appearance_icon_pack)
        }

        if (!::showPinnedAppsLabelsKey.isInitialized) {
            showPinnedAppsLabelsKey = context.getString(R.string.appearance_show_pinned_apps_labels)
        }

        if (!::showAppNamesInSearchResultKey.isInitialized) {
            showAppNamesInSearchResultKey =
                context.getString(R.string.appearance_show_app_names_in_search_result)
        }
    }
}