package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kenneth.app.starlightlauncher.api.IconPack
import java.lang.UnsupportedOperationException

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

    override fun getIconOf(resolveInfo: ResolveInfo) = getIconOf(resolveInfo.activityInfo)

    override fun getIconOf(activityInfo: ActivityInfo): Bitmap =
        activityInfo.loadIcon(packageManager).toBitmap()

    override fun getIconOf(launcherActivityInfo: LauncherActivityInfo): Bitmap =
        launcherActivityInfo.getIcon(0).toBitmap()

    override fun getIconOf(applicationInfo: ApplicationInfo): Bitmap =
        applicationInfo.loadIcon(packageManager).toBitmap()
}
