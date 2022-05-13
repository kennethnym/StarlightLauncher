package kenneth.app.starlightlauncher.setup.permission

import android.Manifest

/**
 * Defines manifest permissions that can be requested using system permission dialogs.
 */
internal val REQUIRED_MANIFEST_PERMISSIONS = listOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
)
