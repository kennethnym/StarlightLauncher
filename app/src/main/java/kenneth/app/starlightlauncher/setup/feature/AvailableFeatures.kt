package kenneth.app.starlightlauncher.setup.feature

import androidx.annotation.BoolRes
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.PREF_MEDIA_CONTROL_ENABLED

/**
 * Describes an available feature in the feature setup step.
 */
internal data class Feature(
    @StringRes
    val name: Int,

    @StringRes
    val description: Int,

    /**
     * The key of this feature in shared preferences.
     */
    val key: Preferences.Key<Boolean>,

    /**
     * Whether this feature is enabled by default
     */
    @BoolRes
    val defaultEnabled: Int,
)

/**
 * Defines available features that users can enable in the feature setup step.
 */
internal val AVAILABLE_FEATURES = listOf(
    Feature(
        name = R.string.feature_name_media_control,
        description = R.string.feature_description_media_control,
        key = PREF_MEDIA_CONTROL_ENABLED,
        defaultEnabled = R.bool.default_media_control_enabled,
    ),
)
