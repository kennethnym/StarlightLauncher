package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

@AndroidEntryPoint
class AdaptiveColorTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs), LifecycleObserver {
    constructor(context: Context) : this(context, null)

    @Inject
    lateinit var appState: AppState

    init {
//        activity?.lifecycle?.addObserver(this)
    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        setTextColor(appState.adaptiveTheme.adaptiveTextColor)
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    private fun onResume() {
//        setTextColor(appState.adaptiveTheme.adaptiveTextColor)
//    }
}