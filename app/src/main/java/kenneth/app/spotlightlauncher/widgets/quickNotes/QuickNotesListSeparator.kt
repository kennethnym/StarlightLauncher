package kenneth.app.spotlightlauncher.widgets.quickNotes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

// List separator should have 30% opacity
private const val SEPARATOR_OPACITY = 0x4D

@AndroidEntryPoint
class QuickNotesListSeparator(context: Context, attrs: AttributeSet) :
    View(context, attrs), LifecycleObserver {
    @Inject
    lateinit var appState: AppState

    init {
        setBackgroundColor(
            ColorUtils.setAlphaComponent(
                appState.adaptiveTextColor,
                SEPARATOR_OPACITY
            )
        )
        activity?.lifecycle?.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        setBackgroundColor(
            ColorUtils.setAlphaComponent(
                appState.adaptiveTextColor,
                SEPARATOR_OPACITY
            )
        )
    }
}