package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class AppSearcher @Inject constructor(
    @ActivityContext private val context: Context,
) {
    private val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps: List<ResolveInfo>
        get() = context.packageManager.queryIntentActivities(mainIntent, 0)
            .filter { notSystemApps(it) }

    fun searchApps(searchRegex: Regex) = apps
        .filter { it.loadLabel(context.packageManager).contains(searchRegex) }
        .sortedWith(appRanker(searchRegex))

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
}