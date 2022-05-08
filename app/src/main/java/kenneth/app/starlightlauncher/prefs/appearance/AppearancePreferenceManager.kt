package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppearancePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultIconPack = DefaultIconPack(context)

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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
