package kenneth.app.spotlightlauncher.api.utils

import android.content.res.Resources

val Int.dp
    get() = this * Resources.getSystem().displayMetrics.density.toInt()
