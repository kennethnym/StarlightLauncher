package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject
import kotlin.math.max

/**
 * A simple LinearLayout wrapper that
 * contains DateTimeView and media control widget if there is media currently playing.
 *
 * Contains a layoutWeight getter/setter to enable ObjectAnimator animation.
 */
@AndroidEntryPoint
class DateTimeViewContainer(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var appState: AppState

    var layoutWeight: Float
        get() = (layoutParams as LayoutParams).weight
        set(newWeight) {
            val newLayoutParams = (layoutParams as LayoutParams).apply {
                weight = newWeight
            }
            layoutParams = newLayoutParams
        }

    init {
        setOnLongClickListener {
            BindingRegister.activityMainBinding.launcherOptionMenu.show()
            HANDLED
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BindingRegister.activityMainBinding.pageScrollView
            .addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                val dateTimeViewScale = max(
                    0f,
                    (y - view.y) / (y - appState.halfScreenHeight)
                )

                Log.d("hub", "scale $dateTimeViewScale")

                scaleX = dateTimeViewScale
                scaleY = dateTimeViewScale
            }
    }
}