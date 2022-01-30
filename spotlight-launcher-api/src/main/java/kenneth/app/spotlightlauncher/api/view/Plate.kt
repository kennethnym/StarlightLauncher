package kenneth.app.spotlightlauncher.api.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Choreographer
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import kenneth.app.spotlightlauncher.api.R
import kenneth.app.spotlightlauncher.api.utils.BlurHandler

private const val DEFAULT_USE_ROUNDED_CORNERS = true

open class Plate(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    /**
     * The [BlurHandler] that should be handling the blur effect of this [Plate].
     * Must be injected before [Plate] is mounted.
     */
    private lateinit var blurHandler: BlurHandler
    private val blurAmount: Int

    private val blurBackground: ImageView =
        ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.MATRIX
            clipToOutline = true
        }.also { addView(it) }

    private var shouldBlur = true

    init {
        context.obtainStyledAttributes(
            intArrayOf(
                R.attr.plateColor,
                R.attr.blurAmount,
            )
        ).run {
            try {
                blurBackground.setColorFilter(
                    getColor(
                        0,
                        context.getColor(android.R.color.transparent)
                    )
                )

                @SuppressLint("ResourceType")
                blurAmount = getInt(1, 20)
            } finally {
                recycle()
            }
        }

        context.obtainStyledAttributes(attrs, R.styleable.Plate).run {
            try {
                if (getBoolean(R.styleable.Plate_useRoundedCorners, DEFAULT_USE_ROUNDED_CORNERS)) {
                    clipToOutline = true
                    background = ContextCompat.getDrawable(context, R.drawable.plate_background)
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        shouldBlur = true
        Choreographer.getInstance().postFrameCallbackDelayed(::frameCallback, 1000 / 120)
    }

    override fun onDetachedFromWindow() {
        shouldBlur = false
        super.onDetachedFromWindow()
    }

    fun blurWith(blurHandler: BlurHandler) {
        this.blurHandler = blurHandler
    }

    private fun frameCallback(ms: Long) {
        if (shouldBlur) {
            if (::blurHandler.isInitialized) {
                blurHandler.blurView(blurBackground, blurAmount)
            }
            Choreographer.getInstance().postFrameCallback(::frameCallback)
        }
    }
}