package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.datastore.preferences.core.booleanPreferencesKey
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.util.BlurHandler

private const val DEFAULT_USE_ROUNDED_CORNERS = true

val PREF_KEY_BLUR_EFFECT_ENABLED = booleanPreferencesKey("appearance_blur_effect_enabled")

open class Plate(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    @IntRange(from = 0x00, to = 0xFF)
    var backgroundAlpha: Int = 0
        set(alpha) {
            field = alpha
            if (blurHandler?.isBlurEffectEnabled == false) {
                plateColor = ColorUtils.setAlphaComponent(plateColor, alpha)
                blurBackground.setBackgroundColor(plateColor)
            }
        }

    private var blurHandler: BlurHandler? = null

    private val blurBackground: ImageView =
        ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.FIT_XY
            clipToOutline = true
        }.also { addView(it) }

    @ColorInt
    private var plateColor: Int

    init {
        context.obtainStyledAttributes(intArrayOf(R.attr.plateColor)).run {
            try {
                plateColor = getColor(
                    0,
                    context.getColor(android.R.color.transparent)
                )
                backgroundAlpha = plateColor.alpha
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

        startBlur()
    }

    override fun onDetachedFromWindow() {
        blurHandler?.unregisterPlate(this)
        super.onDetachedFromWindow()
    }

    fun blurWith(blurHandler: BlurHandler) {
        this.blurHandler = blurHandler.also {
            it.registerPlate(this)
        }
    }

    fun startBlur() {
        blurBackground.setColorFilter(plateColor)
        Choreographer.getInstance().postFrameCallbackDelayed(::frameCallback, 1000 / 120)
    }

    private fun frameCallback(ms: Long) {
        if (blurHandler?.isBlurEffectEnabled == true) {
            blurHandler?.blurView(blurBackground)
            Choreographer.getInstance().postFrameCallbackDelayed(::frameCallback, 1000 / 120)
        } else {
            blurBackground.apply {
                setImageBitmap(null)
                setBackgroundColor(plateColor)
            }
        }
    }
}
