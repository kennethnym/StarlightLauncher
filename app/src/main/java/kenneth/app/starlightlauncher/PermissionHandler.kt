package kenneth.app.starlightlauncher

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

typealias PermissionRequestResultCallback = (Boolean) -> Unit

/**
 * Handles permission logic for a given [Activity]
 */
@ActivityScoped
class PermissionHandler @Inject constructor() {
    /**
     * A map that corresponds [PermissionRequestResultCallback] to the permission that they are
     * interested in.
     */
    private val callbacks = mutableMapOf<String, MutableList<PermissionRequestResultCallback>>()

    private var requestedPermission = ""

    private lateinit var permissionRequestLauncher: ActivityResultLauncher<String>

    /**
     * Registers the given callback to listen for result of request for [permission].
     */
    fun addListener(permission: String, callback: PermissionRequestResultCallback) {
        callbacks[permission]?.add(callback) ?: run {
            callbacks[permission] = mutableListOf(callback)
        }
    }

    /**
     * Registers the activity that [PermissionHandler] needs to handle permission requests for.
     */
    fun handlePermissionRequestsForActivity(activity: ComponentActivity) {
        permissionRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::handleRequestResult
        )
    }

    /**
     * Instructs [PermissionHandler] to request for [permission].
     */
    fun requestPermission(permission: String) {
        requestedPermission = permission
        permissionRequestLauncher.launch(permission)
    }

    private fun handleRequestResult(isGranted: Boolean) {
        callbacks[requestedPermission]
            ?.forEach { it.invoke(isGranted) }
    }
}