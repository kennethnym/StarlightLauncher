package kenneth.app.spotlightlauncher.widgets

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.SpotlightLauncherApiImpl
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.WidgetHolder
import kenneth.app.spotlightlauncher.api.intent.SpotlightLauncherIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcher: SpotlightLauncherApi,
) {
    /**
     * Maps package names of widgets to the corresponding instances of widgets.
     */
    private val loadedWidgets = mutableMapOf<String, WidgetHolder>()

    fun loadWidgets() {
        with(context.packageManager) {
            queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(SpotlightLauncherIntent.CATEGORY_WIDGET)
                },
                PackageManager.GET_META_DATA,
            ).forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val packageRes =
                        context.packageManager.getResourcesForApplication(packageName)

                    val widgetName = packageRes.getString(
                        packageRes.getIdentifier("widget_name", "string", packageName)
                    )
                } catch (ignored: Exception) {

                }
            }
        }
    }
}