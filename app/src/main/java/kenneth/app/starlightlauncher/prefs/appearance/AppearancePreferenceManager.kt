package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import javax.inject.Inject
import javax.inject.Singleton

private object DefaultValue {
    const val SHOW_PINNED_APPS_LABELS = true
    const val SHOW_APP_NAMES_IN_SEARCH_RESULT = true
}

@Singleton
class AppearancePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultIconPack = DefaultIconPack(context)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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

    val showPinnedAppsLabelsKey by lazy {
        context.getString(R.string.appearance_show_pinned_apps_labels)
    }

    val showAppNamesInSearchResultKey by lazy {
        context.getString(R.string.appearance_show_app_names_in_search_result)
    }

    val iconPackPrefKey by lazy {
        context.getString(R.string.appearance_icon_pack)
    }

    /**
     * The icon pack to use, if user has picked any. null if user has not picked any icon pack.
     */
    var iconPack: IconPack =
        sharedPreferences.getString(iconPackPrefKey, null)
            ?.let {
                InstalledIconPack(context, it)
            }
            ?: defaultIconPack
        private set

    /**
     * An alias to SharedPreferences.registerOnSharedPreferenceChangeListener
     */
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun changeIconPack(iconPack: InstalledIconPack) {
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

        iconPack = DefaultIconPack(context)
    }
}
