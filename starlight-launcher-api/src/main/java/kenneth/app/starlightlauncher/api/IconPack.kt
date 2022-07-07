package kenneth.app.starlightlauncher.api

import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

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
     * @return The [Drawable] representation of the icon.
     */
    fun getIconOf(
        launcherActivityInfo: LauncherActivityInfo,
        user: UserHandle = Process.myUserHandle(),
    ): Drawable

    /**
     * Retrieves the icon of [applicationInfo].
     *
     * @return The [Drawable] representation of the icon.
     */
    fun getIconOf(
        applicationInfo: ApplicationInfo,
        user: UserHandle = Process.myUserHandle(),
    ): Drawable
}
