package kenneth.app.starlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets

fun Int.toDp() = this / Resources.getSystem().displayMetrics.density.toInt()

fun Int.toPx() = this * Resources.getSystem().displayMetrics.density.toInt()

val WindowInsets.navBarHeight: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getInsets(WindowInsets.Type.systemBars()).bottom
        else
            systemWindowInsetBottom
