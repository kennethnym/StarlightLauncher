package kenneth.app.starlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets

internal fun Int.toDp() = this / Resources.getSystem().displayMetrics.density.toInt()

internal fun Int.toPx() = this * Resources.getSystem().displayMetrics.density.toInt()
