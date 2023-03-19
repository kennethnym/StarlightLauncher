package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kenneth.app.starlightlauncher.BindingRegister

/**
 * Subclasses [AppWidgetHostView] to avoid scrolling conflict with the widget list.
 */
internal class LauncherAppWidgetHostView(
    context: Context,
    private val bindingRegister: BindingRegister
) : AppWidgetHostView(context) {
    /**
     * The parent scrollable [View] that contains this host view.
     * For example, it can be a RecyclerView that contains a list of
     * widgets.
     */
    var scrollingParent: ViewGroup? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        if (bindingRegister.mainScreenBinding.widgetsPanel.isEditModeEnabled)
            true
        else {
            scrollingParent?.let {
                it.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
}
