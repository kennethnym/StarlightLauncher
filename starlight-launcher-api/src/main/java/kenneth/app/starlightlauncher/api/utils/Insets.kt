package kenneth.app.starlightlauncher.api.utils

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Cross-version compatible version of [android.graphics.Insets]
 */
data class Insets(
    val top: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
) {
    @RequiresApi(Build.VERSION_CODES.Q)
    constructor(inset: android.graphics.Insets) : this(
        inset.top,
        inset.bottom,
        inset.left,
        inset.right
    )
}
