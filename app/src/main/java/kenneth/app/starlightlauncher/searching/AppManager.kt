package kenneth.app.starlightlauncher.searching

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.searching.utils.compareStringsWithRegex
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Comparator

typealias AppInstalledCallback = (app: ResolveInfo) -> Unit

/**
 * The signature of the callback that will be called whenever an app is removed.
 * Register the callback using [AppManager.addOnAppRemovedListener]
 */
typealias AppRemovedCallback = (uninstalledPackageName: String) -> Unit

@Singleton
class AppManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : Observable() {
    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    private val currentAppList: MutableList<ResolveInfo>
    private val appLabels: MutableList<String>

    val apps
        get() = currentAppList.toList()

    init {
        val (appLabels, apps) = context.packageManager.queryIntentActivities(mainIntent, 0)
            .filter(::notSystemApps)
            .map { Pair(it.loadLabel(context.packageManager).toString(), it) }
            .unzip()

        currentAppList = apps.toMutableList()
        this.appLabels = appLabels.toMutableList()
    }

    fun searchApps(searchRegex: Regex) = apps
        .filterIndexed { i, _ -> appLabels[i].contains(searchRegex) }
        .sortedWith(appRanker(searchRegex))

    /**
     * Adds a listener that is called whenever a new app is installed.
     * The [ResolveInfo] of the installed app is passed to the listener.
     */
    fun addOnAppInstalledListener(listener: AppInstalledCallback) {
        addObserver { o, arg ->
            if (o is AppManager && arg is Intent && arg.action == Intent.ACTION_PACKAGE_ADDED) {
                arg.data
                    ?.let { packageNameOfUri(it) }
                    ?.let { resolvePackageName(it) }
                    ?.let(listener)
            }
        }
    }

    /**
     * Adds a listener that is called whenever an app is removed.
     * The [ResolveInfo] of the removed app is passed to the listener.
     */
    fun addOnAppRemovedListener(listener: AppRemovedCallback) {
        addObserver { o, arg ->
            if (o is AppManager && arg is Intent && arg.action == Intent.ACTION_PACKAGE_REMOVED) {
                arg.data
                    ?.let { packageNameOfUri(it) }
                    ?.let(listener)
            }
        }
    }

    /**
     * Uninstall the app with the given package name
     * @param packageName The package name of the app to be uninstalled.
     */
    fun uninstall(packageName: String) {
        val uninstallIntent = Intent(
            Intent.ACTION_DELETE,
            Uri.fromParts("package", packageName, null),
        )
        context.startActivity(uninstallIntent)
    }

    private fun notSystemApps(appInfo: ResolveInfo) =
        (appInfo.activityInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 1

    /**
     * appRanker ranks apps in the list based on the search query.
     */
    private fun appRanker(searchRegex: Regex): Comparator<ResolveInfo> {
        return Comparator { app1, app2 ->
            val appName1 = app1.loadLabel(context.packageManager)
            val appName2 = app2.loadLabel(context.packageManager)

            compareStringsWithRegex(appName1.toString(), appName2.toString(), searchRegex)
        }
    }

    private fun packageNameOfUri(uri: Uri) = uri.schemeSpecificPart

    private fun resolvePackageName(packageName: String) =
        Intent().apply {
            `package` = packageName
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.run {
            context.packageManager
                ?.resolveActivity(this, 0)
        }

    inner class PackageObserver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val receivedPackageName = intent?.data?.schemeSpecificPart
            when (intent?.action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    currentAppList.removeAt(
                        apps.indexOfFirst {
                            it.activityInfo.packageName == receivedPackageName
                        }
                    )

                    setChanged()
                    notifyObservers(intent)
                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    val packageLauncherIntent = Intent().apply {
                        `package` = receivedPackageName
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    context?.packageManager
                        ?.resolveActivity(packageLauncherIntent, 0)
                        ?.let { currentAppList.add(it) }

                    setChanged()
                    notifyObservers(intent)
                }
            }
            Log.d("hub", "ACTION ${intent?.action}")
            Log.d("hub", "ACTION ${intent?.data}")
        }
    }
}