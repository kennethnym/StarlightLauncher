package kenneth.app.starlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.view.WindowInsets

val Int.dp: Int
    get() = this * Resources.getSystem().displayMetrics.density.toInt()

val WindowInsets.navBarHeight: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getInsets(WindowInsets.Type.systemBars()).bottom
        else
            systemWindowInsetBottom
