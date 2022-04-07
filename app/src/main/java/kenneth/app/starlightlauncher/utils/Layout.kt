package kenneth.app.starlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets

val Int.dp: Int
    get() = this / Resources.getSystem().displayMetrics.density.toInt()

val Int.px: Int
    get() = this * (Resources.getSystem().displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)

val WindowInsets.navBarHeight: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getInsets(WindowInsets.Type.systemBars()).bottom
        else
            systemWindowInsetBottom
