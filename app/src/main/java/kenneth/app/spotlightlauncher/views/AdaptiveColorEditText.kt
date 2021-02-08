package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

private const val HINT_TEXT_OPACITY = 0x80

/**
 * An [EditText] that changes tint based on [AppState.adaptiveTextColor].
 * Can be used to display a font icon.
 */
@AndroidEntryPoint
class AdaptiveColorEditText(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatEditText(context, attrs), LifecycleObserver {
    @Inject
    lateinit var appState: AppState

    init {
        activity?.lifecycle?.addObserver(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setHintTextColor(
            ColorUtils.setAlphaComponent(
                appState.adaptiveTextColor,
                HINT_TEXT_OPACITY
            )
        )
        setTextColor(appState.adaptiveTextColor)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        setHintTextColor(
            ColorUtils.setAlphaComponent(
                appState.adaptiveTextColor,
                HINT_TEXT_OPACITY
            )
        )
        setTextColor(appState.adaptiveTextColor)
    }
}