package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.BlurHandler
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class BlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {
    @Inject
    lateinit var choreographer: Choreographer

    @Inject
    lateinit var blurHandler: BlurHandler

    @Inject
    lateinit var appState: AppState

    private var blurAmount: Int

    private var blurImageView: WeakReference<ImageView>

    private val blurImageViewId: Int = generateViewId()

    /**
     * Determines if blur should be continuously updated
     */
    private var shouldUpdateBlur = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BlurView,
            0, 0
        ).apply {
            try {
                blurAmount = getInteger(R.styleable.BlurView_blurAmount, 0)
            } finally {
                recycle()
            }
        }

        blurImageView = WeakReference(
            ImageView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                )
                scaleType = ImageView.ScaleType.FIT_XY
                id = blurImageViewId
                clipToOutline = true
                setBackgroundResource(R.drawable.rounded_background)
            }.also { addView(it, 0) }
        )
    }

    fun startBlur() {
        shouldUpdateBlur = true
        choreographer.postFrameCallbackDelayed(::frameCallback, 1000 / 60)
    }

    fun pauseBlur() {
        shouldUpdateBlur = false
    }

    private fun frameCallback(frameTimeNanos: Long) {
        if (shouldUpdateBlur) {
            if (blurImageView.get() == null) {
                getImageView()
            }

            blurImageView.get()?.let {
                blurHandler.blurView(it, blurAmount)
                choreographer.postFrameCallbackDelayed(::frameCallback, 1000 / 60)
            }
        }
    }

    private fun getImageView() {
        blurImageView = WeakReference(findViewById(blurImageViewId))
    }
}