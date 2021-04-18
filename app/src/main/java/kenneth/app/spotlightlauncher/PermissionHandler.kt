package kenneth.app.spotlightlauncher

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

typealias PermissionRequestResultCallback = (Boolean) -> Unit

/**
 * Handles permission logic for a given [Activity]
 */
object PermissionHandler {
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
    fun handlePermissionForActivity(activity: ComponentActivity) {
        permissionRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::handleRequestResult
        )
    }

    /**
     * Instructs [PermissionHandler] to request for [permission].
     */
    fun requestPermission(permission: String) {
        permissionRequestLauncher.launch(permission)
    }

    private fun handleRequestResult(isGranted: Boolean) {
        callbacks[requestedPermission]
            ?.forEach { it.invoke(isGranted) }
    }
}