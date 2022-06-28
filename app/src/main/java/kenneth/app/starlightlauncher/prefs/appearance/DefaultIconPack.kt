package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import kenneth.app.starlightlauncher.api.IconPack

/**
 * An [IconPack] representing the default icon pack.
 * When querying icon of an app, this icon pack will simply return the default icon of it.
 */
internal class DefaultIconPack(context: Context) : IconPack {
    private val packageManager = context.packageManager

    override val icon: Bitmap
        get() = throw UnsupportedOperationException()

    override val name: String
        get() = "Default"

    private val loadedIcons = mutableMapOf<String, Drawable>()

    override fun getIconOf(launcherActivityInfo: LauncherActivityInfo, user: UserHandle) =
        loadedIcons.getOrPut(launcherActivityInfo.applicationInfo.packageName) {
            val icon = launcherActivityInfo.getIcon(0)
            packageManager.getUserBadgedIcon(icon, user)
        }

    override fun getIconOf(applicationInfo: ApplicationInfo, user: UserHandle): Drawable =
        loadedIcons.getOrPut(applicationInfo.packageName) {
            val icon = applicationInfo.loadIcon(packageManager)
            packageManager.getUserBadgedIcon(icon, user)
        }
}
