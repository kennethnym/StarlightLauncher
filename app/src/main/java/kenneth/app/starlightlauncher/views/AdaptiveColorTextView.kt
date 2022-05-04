package kenneth.app.starlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import javax.inject.Inject

@AndroidEntryPoint
internal class AdaptiveColorTextView(context: Context, attrs: AttributeSet?) :
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