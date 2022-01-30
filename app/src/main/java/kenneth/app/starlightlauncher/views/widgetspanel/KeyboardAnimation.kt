package kenneth.app.starlightlauncher.views.widgetspanel

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import androidx.annotation.RequiresApi
import kenneth.app.starlightlauncher.AppState
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
class KeyboardAnimation @Inject constructor(
    private val widgetsPanel: WidgetsPanel,
    private val appState: AppState,
) :
    WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
    private var shouldUnsetViewAfterAnimation = false
    private var viewToAvoid: View? = null
    private var viewOriginalY = 0f

    override fun onProgress(
        insets: WindowInsets,
        runningAnimations: MutableList<WindowInsetsAnimation>
    ): WindowInsets {
        viewToAvoid?.let {
            val imeInsetsBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
            val insetFromTop = appState.screenHeight - imeInsetsBottom

            // check if keyboard is above the view to be avoided
            if (insetFromTop <= viewOriginalY) {
                // move widget panel up if true
                widgetsPanel.translationY = -(viewOriginalY - insetFromTop)
            }
        }

        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimation) {
        if (shouldUnsetViewAfterAnimation) {
            viewToAvoid = null
            shouldUnsetViewAfterAnimation = false
        }
        super.onEnd(animation)
    }

    fun avoidView(view: View) {
        viewToAvoid = view
        viewOriginalY = intArrayOf(0, 0).run {
            view.getLocationOnScreen(this)
            this[1].toFloat() + view.height
        }
    }

    fun stopAvoidingView() {
        if (widgetsPanel.rootWindowInsets.isVisible(WindowInsets.Type.ime())) {
            shouldUnsetViewAfterAnimation = true
        } else {
            viewToAvoid = null
        }
    }
}