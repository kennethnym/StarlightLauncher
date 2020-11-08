package kenneth.app.spotlightlauncher.utils

import android.content.res.Resources

fun Int.toPx(resources: Resources): Int {
    val density = resources.displayMetrics.density
    return (this * density).toInt()
}
