package kenneth.app.starlightlauncher.setup.permission

import android.Manifest
import androidx.annotation.StringRes
import kenneth.app.starlightlauncher.R

/**
 * Defines a permission that the launcher needs for some feature to work.
 */
internal data class Permission(
    @StringRes
    val name: Int,

    @StringRes
    val description: Int,
)

internal val REQUIRED_MANIFEST_PERMISSIONS = listOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
)

internal val REQUIRED_PERMISSIONS = listOf(
    Permission(
        name = R.string.permission_name_external_storage,
        description = R.string.permission_description_external_storage,
    ),
    Permission(
        name = R.string.permission_name_notification_access,
        description = R.string.permission_description_notification_access,
    ),
)
