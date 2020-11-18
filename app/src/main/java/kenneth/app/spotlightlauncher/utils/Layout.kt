package kenneth.app.spotlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import kenneth.app.spotlightlauncher.R

fun Int.toPx(resources: Resources): Int {
    val density = resources.displayMetrics.density
    return (this * density).toInt()
}

@RequiresApi(Build.VERSION_CODES.R)
class KeyboardAnimationCallback(private val rootView: View) :
    WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
    private var shouldAnimateKeyboard = false
    private var paddingBottom = rootView.paddingBottom

    init {
        rootView.findViewById<TextView>(R.id.search_box)
            .addTextChangedListener { text ->
                shouldAnimateKeyboard = text?.isNotEmpty() ?: false
                rootView.updatePadding(
                    bottom =
                    if (shouldAnimateKeyboard) rootView.rootWindowInsets
                        .getInsets(WindowInsets.Type.ime())
                        .bottom
                    else 0
                )
            }
    }

    override fun onProgress(
        insets: WindowInsets,
        animations: MutableList<WindowInsetsAnimation>
    ): WindowInsets {
        if (shouldAnimateKeyboard) {
            rootView.updatePadding(bottom = paddingBottom + insets.getInsets(WindowInsets.Type.ime()).bottom)
        }
        return insets
    }
}
