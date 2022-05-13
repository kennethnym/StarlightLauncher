package kenneth.app.starlightlauncher.setup.permission

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.FragmentSetupPermissionBinding
import kenneth.app.starlightlauncher.databinding.SetupPermissionItemBinding
import kenneth.app.starlightlauncher.views.NotificationListenerStub

/**
 * This setup fragment informs the users the permissions Starlight Launcher needs for some features to work.
 */
internal class PermissionFragment : Fragment() {
    private val permissionRequestActivityLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::onPermissionRequestResult
        )

    private val permissionItemBindings = mutableMapOf<String, SetupPermissionItemBinding>()

    private var notificationListenerPermissionItemBinding: SetupPermissionItemBinding? = null

    private var grantedPermissions = mutableSetOf<String>()

    private var currentlyRequestedPermission: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSetupPermissionBinding.inflate(inflater).run {
        REQUIRED_MANIFEST_PERMISSIONS.forEach {
            SetupPermissionItemBinding.inflate(inflater, root, true).apply {
                permissionName = R.string.permission_name_external_storage
                permissionDescription = R.string.permission_description_external_storage
                isPermissionGranted =
                    context?.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
                root.setOnClickListener { _ -> requestManifestPermission(it) }
            }.also { binding ->
                permissionItemBindings[it] = binding
                if (binding.isPermissionGranted == true) {
                    grantedPermissions += it
                }
            }
        }

        SetupPermissionItemBinding.inflate(inflater, root, true).apply {
            permissionName = R.string.permission_name_notification_access
            permissionDescription = R.string.permission_description_notification_access
            root.setOnClickListener { requestNotificaitonListenerPermission() }
        }.also { notificationListenerPermissionItemBinding = it }

        root
    }

    override fun onResume() {
        super.onResume()
        notificationListenerPermissionItemBinding?.isPermissionGranted =
            isNotificationListenerEnabled()
    }

    /**
     * Determines if the registered notification listener is enabled.
     * If true, then the currently playing media is accessible.
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val context = this.context ?: return false
        val notificationListenerStr =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")

        return notificationListenerStr != null && notificationListenerStr.contains(context.packageName)
    }

    private fun onPermissionRequestResult(isGranted: Boolean) {
        if (isGranted) {
            currentlyRequestedPermission?.let {
                grantedPermissions += it
                permissionItemBindings[it]?.isPermissionGranted = true
                currentlyRequestedPermission = null
            }
        }
    }

    private fun requestManifestPermission(permission: String) {
        if (currentlyRequestedPermission == null && !grantedPermissions.contains(permission)) {
            currentlyRequestedPermission = permission
            permissionRequestActivityLauncher.launch(permission)
        }
    }

    private fun requestNotificaitonListenerPermission() {
        val context = this.context ?: return
        val notificationListenerStubComponentName = ComponentName(
            context,
            NotificationListenerStub::class.java
        ).flattenToString()
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
                ).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        notificationListenerStubComponentName
                    )
                }
            else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                val fragmentArgKey = ":settings:fragment_args_key"
                putExtra(fragmentArgKey, notificationListenerStubComponentName)
                putExtra(":settings:show_fragment_args", Bundle().apply {
                    putString(fragmentArgKey, notificationListenerStubComponentName)
                })
            }

        startActivity(intent)
    }
}