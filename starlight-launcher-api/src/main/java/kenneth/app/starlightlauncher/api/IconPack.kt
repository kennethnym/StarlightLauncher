package kenneth.app.starlightlauncher.api

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap

/**
 * Describes an IconPack.
 */
interface IconPack {
    /**
     * The icon of the icon pack.
     */
    val icon: Bitmap

    /**
     * The name of the icon pack.
     */
    val name: String

    /**
     * Retrieves the icon of the given [ResolveInfo].
     */
    fun getIconOf(resolveInfo: ResolveInfo): Bitmap

    /**
     * Retrieves the icon of the given [ActivityInfo]
     */
    fun getIconOf(activityInfo: ActivityInfo): Bitmap

    fun getIconOf(launcherActivityInfo: LauncherActivityInfo): Bitmap

    fun getIconOf(applicationInfo: ApplicationInfo): Bitmap
}
