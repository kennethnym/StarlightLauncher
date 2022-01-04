package kenneth.app.spotlightlauncher.api.view

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Choreographer
import android.widget.FrameLayout
import android.widget.ImageView
import kenneth.app.spotlightlauncher.api.R

open class BlurView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val blurImageView: ImageView

    private val blurImageViewId: Int = generateViewId()
    private val blurImageTranslationMatrix = Matrix()

    private val blurAmount: Int

    private val blurHandler = BlurHandler(context)

    init {
        context.obtainStyledAttributes(attrs, intArrayOf(R.attr.blurAmount))
            .run {
                try {
                    blurAmount = getInteger(0, 0)
                } finally {
                    recycle()
                }
            }

        blurImageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.FIT_XY
            id = blurImageViewId
            clipToOutline = true
            scaleType = ImageView.ScaleType.MATRIX
            imageMatrix = blurImageTranslationMatrix
            background = this@BlurView.background
        }
    }

    private fun startBlur() {
        Choreographer.getInstance()
            .postFrameCallbackDelayed(::frameCallback, 1000 / 120L)
    }

    private fun frameCallback(frameTimeNano: Long) {
        blurHandler.blurView(blurImageView, blurAmount)
        startBlur()
    }
}