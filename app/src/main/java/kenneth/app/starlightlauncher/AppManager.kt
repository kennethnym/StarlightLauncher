package kenneth.app.starlightlauncher

import android.content.*
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.LauncherEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages apps installed on the phone. Use this class to:
 * - query installed apps
 * - listen to when apps are removed/changed/installed
 */
@Singleton
internal class AppManager @Inject constructor(
    @ApplicationContext context: Context,
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val launcherEventChannel: LauncherEventChannel,
    private val applicationScope: CoroutineScope,
) {
    /**
     * Stores all apps installed on the device.
     */
    private val allApps =
        mutableMapOf<UserHandle, MutableMap<ComponentName, LauncherActivityInfo>>()

    val installedApps
        get() = allApps.values.flatMap { it.values }

    /**
     * Maps app package names to their corresponding labels.
     */
    private val appLabels = mutableMapOf<String, String>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null) return
            when (intent?.action) {
                Intent.ACTION_MANAGED_PROFILE_ADDED -> {
                    val user =
                        if (Build.VERSION.SDK_INT >= 33)
                            intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
                        else
                            UserHandle(intent.getParcelableExtra(Intent.EXTRA_USER))

                    user?.let { loadAppsForUser(it) }
                }
                Intent.ACTION_MANAGED_PROFILE_REMOVED -> {
                    val removedUser =
                        if (Build.VERSION.SDK_INT >= 33)
                            intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
                        else
                            UserHandle(intent.getParcelableExtra(Intent.EXTRA_USER))

                    allApps.remove(removedUser)
                }
            }
        }
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            allApps[user]?.entries?.removeIf { it.key.packageName == packageName }
            appLabels.remove(packageName)

            applicationScope.launch {
                launcherEventChannel.add(LauncherEvent.AppRemoved(packageName))
            }
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return
            // get the apps for the user
            // if it doesn't exist, create a new map to store the apps
            val userApps =
                allApps[user] ?: mutableMapOf<ComponentName, LauncherActivityInfo>().also {
                    allApps[user] = it
                }

            val activities = launcherApps.getActivityList(packageName, user)
            activities.forEach {
                userApps[it.componentName] = it
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }

            applicationScope.launch {
                launcherEventChannel.add(LauncherEvent.NewAppsInstalled(activities))
            }
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return
            val userApps = allApps[user] ?: return
            val activities = launcherApps.getActivityList(packageName, user)

            userApps.entries.removeIf { it.key.packageName == packageName }
            appLabels.remove(packageName)
            activities.forEach {
                userApps[it.componentName] = it
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (packageNames == null || user == null) return
            val userApps = allApps[user] ?: return

            packageNames.forEach { packageName ->
                val activities = launcherApps.getActivityList(packageName, user)
                userApps.entries.removeIf { it.key.packageName == packageName }
                activities.forEach {
                    userApps[it.componentName] = it
                    appLabels[it.applicationInfo.packageName] = it.label.toString()
                }
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (packageNames == null || user == null) return

            allApps[user]?.entries?.removeIf { packageNames.contains(it.key.packageName) }
            packageNames.forEach { appLabels.remove(it) }
        }
    }

    init {
        loadApps()
        context.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        })
    }

    fun appLabelOf(packageName: String) = appLabels[packageName]

    fun launcherActivityInfoOf(
        componentName: ComponentName,
        user: UserHandle = Process.myUserHandle()
    ) = allApps[user]?.get(componentName)

    private fun loadApps() {
        userManager.userProfiles.forEach { loadAppsForUser(it) }
        launcherApps.registerCallback(launcherAppsCallback)
    }

    /**
     * Finds apps installed under [user].
     */
    private fun loadAppsForUser(user: UserHandle) {
        val userApps = mutableMapOf<ComponentName, LauncherActivityInfo>().also {
            allApps[user] = it
        }
        launcherApps.getActivityList(null, user)
            .forEach {
                val packageName = it.applicationInfo.packageName
                userApps[it.componentName] = it
                appLabels[packageName] = it.label.toString()
            }
    }
}