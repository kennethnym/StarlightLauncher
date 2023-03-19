package kenneth.app.starlightlauncher.api.util

import android.widget.ImageView
import kenneth.app.starlightlauncher.api.view.Plate

interface BlurHandler {
    val isBlurEffectEnabled: Boolean

    fun registerPlate(plate: Plate)

    fun unregisterPlate(plate: Plate)

    fun blurView(dest: ImageView)
}
