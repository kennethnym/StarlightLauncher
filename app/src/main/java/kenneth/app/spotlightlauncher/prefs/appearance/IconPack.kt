package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.pm.ResolveInfo
import android.graphics.Bitmap

/**
 * Describes an IconPack.
 */
interface IconPack {
    val icon: Bitmap
    val name: String

    fun getIconOf(resolveInfo: ResolveInfo): Bitmap
}
