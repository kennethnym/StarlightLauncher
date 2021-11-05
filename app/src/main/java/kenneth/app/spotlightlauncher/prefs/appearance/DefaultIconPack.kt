package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import java.lang.UnsupportedOperationException

/**
 * An [IconPack] representing the default icon pack.
 * When querying icon of an app, this icon pack will simply return the default icon of it.
 */
class DefaultIconPack(context: Context) : IconPack {
    private val packageManager = context.packageManager

    override val icon: Bitmap
        get() = throw UnsupportedOperationException()

    override val name: String
        get() = "Default"

    override fun getIconOf(resolveInfo: ResolveInfo) =
        resolveInfo.loadIcon(packageManager).toBitmap()
}
