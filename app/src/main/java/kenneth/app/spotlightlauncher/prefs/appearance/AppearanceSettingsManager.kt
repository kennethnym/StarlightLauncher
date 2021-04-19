package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import kenneth.app.spotlightlauncher.R

private object DefaultValue {
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
    val areNamesOfPinnedAppsShown: Boolean
        get() = sharedPreferences.getBoolean(
            showPinnedAppsLabelsKey,
            DefaultValue.SHOW_PINNED_APPS_LABELS
        )

    /**
     * Whether the user chooses to show names of apps in search result.
     */
    val areAppNamesInSearchResult: Boolean
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

    val showPinnedAppsLabelsKey by lazy { context.getString(R.string.appearance_show_pinned_apps_labels) }
    val showAppNamesInSearchResultKey by lazy { context.getString(R.string.appearance_show_app_names_in_search_result) }
    val iconPackPrefKey by lazy { context.getString(R.string.appearance_icon_pack) }

    fun getInstance(context: Context) = this.apply {
        this.context = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (iconPack == null) {
            Log.d("hub", iconPackPrefKey)
            val selectedIconPackPackageName = sharedPreferences.getString(iconPackPrefKey, null)
            Log.d("hub", selectedIconPackPackageName.toString())

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
}