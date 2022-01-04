package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kenneth.app.spotlightlauncher.AppState
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

/**
 * An [ImageView] that changes tint based on [appState.adaptiveTheme.adaptiveTextColor].
 * Can be used to display a font icon.
 */
@AndroidEntryPoint
class AdaptiveColorImageView(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs), LifecycleObserver {
    @Inject
    lateinit var appState: AppState

    init {
        activity?.lifecycle?.addObserver(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setColorFilter(appState.adaptiveTheme.adaptiveTextColor)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        setColorFilter(appState.adaptiveTheme.adaptiveTextColor)
    }
}