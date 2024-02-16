package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kenneth.app.starlightlauncher.LauncherState

/**
 * Subclasses [AppWidgetHostView] to avoid scrolling conflict with the widget list.
 */
internal class LauncherAppWidgetHostView(
    context: Context,
    private val launcherState: LauncherState,
) : AppWidgetHostView(context) {
    /**
     * The parent scrollable [View] that contains this host view.
     * For example, it can be a RecyclerView that contains a list of
     * widgets.
     */
    var scrollingParent: ViewGroup? = null

    var isInWidgetEditMode = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        if (launcherState.isInWidgetEditMode)
            true
        else {
            scrollingParent?.requestDisallowInterceptTouchEvent(true)
            false
        }
}
