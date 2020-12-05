package kenneth.app.spotlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R

fun Int.toPx(resources: Resources): Int {
    val density = resources.displayMetrics.density
    return (this * density).toInt()
}

@RequiresApi(Build.VERSION_CODES.R)
class KeyboardAnimationCallback(activity: MainActivity) :
    WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
    private var shouldAnimateKeyboard = false
    private val pageScrollView = activity.findViewById<NestedScrollView>(R.id.page_scroll_view)
    private var paddingBottom = pageScrollView.paddingBottom

    init {
        activity.findViewById<TextView>(R.id.search_box)
            .addTextChangedListener { text ->
                shouldAnimateKeyboard = text?.isNotEmpty() ?: false
                pageScrollView.updatePadding(
                    bottom =
                    if (shouldAnimateKeyboard) pageScrollView.rootWindowInsets
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
            pageScrollView.updatePadding(bottom = paddingBottom + insets.getInsets(WindowInsets.Type.ime()).bottom)
        }
        return insets
    }
}
