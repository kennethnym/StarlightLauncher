package kenneth.app.spotlightlauncher.animations

import android.util.Log
import android.view.View

interface DimensionAnimatable {
    var targetView: View

    fun setWidth(width: Int)

    fun setHeight(height: Int)
}

class DimensionAnimator : DimensionAnimatable {
    override lateinit var targetView: View

    override fun setWidth(width: Int) {
        Log.d("hub", "width $width")
        targetView.layoutParams.width = width
        targetView.layoutParams = targetView.layoutParams
    }

    override fun setHeight(height: Int) {
        targetView.layoutParams.height = height
        targetView.layoutParams = targetView.layoutParams
    }
}
