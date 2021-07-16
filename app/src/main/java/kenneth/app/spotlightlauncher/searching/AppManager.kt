package kenneth.app.spotlightlauncher.searching

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.Comparator

typealias AppChangedCallback = (app: ResolveInfo) -> Unit

@ActivityScoped
class AppManager @Inject constructor(
    @ActivityContext private val context: Context,
) : Observable() {
    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps =
        context.packageManager.queryIntentActivities(mainIntent, 0)
            .filter(::notSystemApps)
            .toMutableList()

    fun searchApps(searchRegex: Regex) = apps
        .filter { it.loadLabel(context.packageManager).contains(searchRegex) }
        .sortedWith(appRanker(searchRegex))

    /**
     * Adds a listener that is called whenever a new app is installed.
     * The [ResolveInfo] of the installed app is passed to the listener.
     */
    fun addOnAppInstalledListener(listener: AppChangedCallback) {
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
    fun addOnAppRemovedListener(listener: AppChangedCallback) {
        addObserver { o, arg ->
            if (o is AppManager && arg is Intent && arg.action == Intent.ACTION_PACKAGE_REMOVED) {
                arg.data
                    ?.let { packageNameOfUri(it) }
                    ?.let { resolvePackageName(it) }
                    ?.let(listener)
            }
        }
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
                    apps.removeAt(
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
                        ?.let { apps.add(it) }

                    setChanged()
                    notifyObservers(intent)
                }
            }
            Log.d("hub", "ACTION ${intent?.action}")
            Log.d("hub", "ACTION ${intent?.data}")
        }
    }
}