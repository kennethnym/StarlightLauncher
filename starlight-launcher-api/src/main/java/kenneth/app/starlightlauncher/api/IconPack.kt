package kenneth.app.starlightlauncher.api

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
}