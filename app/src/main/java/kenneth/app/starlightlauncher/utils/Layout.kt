package kenneth.app.starlightlauncher.utils

import android.content.res.Resources

internal fun Int.toDp() = this / Resources.getSystem().displayMetrics.density.toInt()

internal fun Int.toPx() = this * Resources.getSystem().displayMetrics.density.toInt()
