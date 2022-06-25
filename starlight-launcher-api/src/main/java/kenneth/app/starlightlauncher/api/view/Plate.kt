package kenneth.app.starlightlauncher.api.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.Choreographer
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.util.BlurHandler

private const val DEFAULT_USE_ROUNDED_CORNERS = true

open class Plate(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var blurHandler: BlurHandler
    private val blurAmount: Int

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val blurBackground: ImageView =
        ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.FIT_XY
            clipToOutline = true
        }.also { addView(it) }

    private var shouldBlur: Boolean

    @ColorInt
    private val plateColor: Int

    init {
        context.obtainStyledAttributes(
            intArrayOf(
                R.attr.plateColor,
                R.attr.blurAmount,
            )
        ).run {
            try {
                plateColor = getColor(
                    0,
                    context.getColor(android.R.color.transparent)
                )

                // detect if the launcher has access to the wallpaper and if blur effect is enabled
                if (
                    context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    sharedPreferences.getBoolean(
                        context.getString(R.string.pref_key_appearance_blur_effect_enabled), true
                    )
                ) {
                    shouldBlur = true
                    // set color filter of the blur effect
                    blurBackground.setColorFilter(plateColor)
                } else {
                    shouldBlur = false
                    // set a simple transparent color because the launcher cannot provide blur effect
                    blurBackground.setBackgroundColor(plateColor)
                }

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
        if (shouldBlur) startBlur()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        shouldBlur = false
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return
        when (key) {
            context.getString(R.string.pref_key_appearance_blur_effect_enabled) -> {
                toggleBlurEffect()
            }
        }
    }

    fun blurWith(blurHandler: BlurHandler) {
        this.blurHandler = blurHandler
    }

    private fun toggleBlurEffect() {
        val key = context.getString(R.string.pref_key_appearance_blur_effect_enabled)
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val blurEffectEnabled = sharedPreferences.getBoolean(key, true)
            if (blurEffectEnabled) {
                startBlur()
            } else {
                turnOffBlur()
            }
        } else {
            turnOffBlur()
            sharedPreferences.edit(commit = true) {
                putBoolean(key, false)
            }
        }
    }

    private fun turnOffBlur() {
        shouldBlur = false
        blurBackground.apply {
            setImageBitmap(null)
            setBackgroundColor(plateColor)
        }
    }

    private fun startBlur() {
        shouldBlur = true
        blurBackground.setColorFilter(plateColor)
        Choreographer.getInstance().postFrameCallbackDelayed(::frameCallback, 1000 / 120)
    }

    private fun frameCallback(ms: Long) {
        if (shouldBlur) {
            if (::blurHandler.isInitialized) {
                blurHandler.blurView(blurBackground, blurAmount)
            }
            startBlur()
        }
    }
}