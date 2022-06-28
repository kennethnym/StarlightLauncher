package kenneth.app.starlightlauncher.api

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

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
     * Retrieves the icon of [launcherActivityInfo].
     *
     * @return The [Bitmap] representation of the icon.
     */
    fun getIconOf(launcherActivityInfo: LauncherActivityInfo): Bitmap

    /**
     * Retrieves the icon of [applicationInfo].
     *
     * @return The [Bitmap] representation of the icon.
     */
    fun getIconOf(applicationInfo: ApplicationInfo): Bitmap
}
