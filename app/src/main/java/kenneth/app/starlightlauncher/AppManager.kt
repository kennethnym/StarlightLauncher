package kenneth.app.starlightlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.LauncherEvent
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
) {
    /**
     * Stores all apps installed on the device.
     */
    private val allApps = mutableListOf<LauncherActivityInfo>()

    val installedApps
        get() = allApps.toList()

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

                    allApps.removeAll { it.user == removedUser }
                }
            }
        }
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return
            allApps.removeAll { it.applicationInfo.packageName == packageName && it.user == user }
            appLabels.remove(packageName)

            launcherEventChannel.add(LauncherEvent.AppRemoved(packageName))
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            allApps.addAll(activities)
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }

            launcherEventChannel.add(LauncherEvent.NewAppsInstalled(activities))
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            if (packageName == null || user == null) return

            val activities = launcherApps.getActivityList(packageName, user)
            allApps.apply {
                removeAll { it.applicationInfo.packageName == packageName && it.user == user }
                addAll(activities)
            }
            appLabels.remove(packageName)
            activities.forEach {
                appLabels[it.applicationInfo.packageName] = it.label.toString()
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (packageNames == null || user == null) return

            packageNames.forEach { packageName ->
                val activities = launcherApps.getActivityList(packageName, user)
                allApps.apply {
                    removeAll { it.applicationInfo.packageName == packageName && it.user == user }
                    addAll(activities)
                }
                activities.forEach {
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

            allApps.removeAll { it.user == user && packageNames.contains(it.applicationInfo.packageName) }
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

    private fun loadApps() {
        userManager.userProfiles.forEach { loadAppsForUser(it) }
        launcherApps.registerCallback(launcherAppsCallback)
    }

    /**
     * Finds apps installed under [user].
     */
    private fun loadAppsForUser(user: UserHandle) {
        launcherApps.getActivityList(null, user)
            .forEach {
                val packageName = it.applicationInfo.packageName
                allApps += it
                appLabels[packageName] = it.label.toString()
            }
    }
}