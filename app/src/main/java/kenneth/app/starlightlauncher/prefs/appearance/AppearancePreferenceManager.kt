package kenneth.app.starlightlauncher.prefs.appearance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.view.PREF_KEY_BLUR_EFFECT_ENABLED
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.datetime.DEFAULT_USE_24HR_CLOCK
import kenneth.app.starlightlauncher.prefs.PREF_KEY_ICON_PACK
import kenneth.app.starlightlauncher.prefs.PREF_KEY_USE_24HR_CLOCK
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles preferences for launcher appearance.
 */
@Singleton
internal class AppearancePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val defaultIconPack = DefaultIconPack(context)
    private var currentIconPack: IconPack? = null

    val isAppDrawerEnabled = context.dataStore.data.map {
        it[PREF_KEY_USE_24HR_CLOCK] ?: DEFAULT_USE_24HR_CLOCK
    }

    /**
     * Whether blur effect is enabled. If not set, the default value
     * is whether the launcher has read external storage permission.
     * If the launcher has permission, then blur effect is enabled by default.
     */
    val isBlurEffectEnabled = context.dataStore.data.map {
        it[PREF_KEY_BLUR_EFFECT_ENABLED]
            ?: (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * The icon pack to use, if user has picked any. null if user has not picked any icon pack.
     */
    val iconPack = context.dataStore.data
        .map { it[PREF_KEY_ICON_PACK] }
        .distinctUntilChanged()
        .map { iconPackPackageName ->
            if (iconPackPackageName != null) {
                InstalledIconPack(
                    iconPackPackageName,
                    context.packageManager,
                    context.resources
                ).also { it.load() }
            } else {
                defaultIconPack
            }
        }

    /**
     * Sets whether blur effect should be enabled.
     *
     * @param enabled Whether blur effect should be enabled
     */
    suspend fun setBlurEffectEnabled(enabled: Boolean) {
        val value =
            if (enabled)
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            else
                false

        context.dataStore.edit {
            it[PREF_KEY_BLUR_EFFECT_ENABLED] = value
        }
    }

    suspend fun setAppDrawerEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PREF_KEY_USE_24HR_CLOCK] = enabled
        }
    }

    suspend fun changeIconPack(iconPack: InstalledIconPack) {
        context.dataStore.edit {
            it[PREF_KEY_ICON_PACK] = iconPack.packageName
        }
    }

    /**
     * Revert the applied icon pack and use default icons instead.
     */
    suspend fun useDefaultIconPack() {
        context.dataStore.edit {
            it.remove(PREF_KEY_ICON_PACK)
        }
        currentIconPack = defaultIconPack
    }
}
