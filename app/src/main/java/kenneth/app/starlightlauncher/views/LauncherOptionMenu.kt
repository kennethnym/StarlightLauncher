package kenneth.app.starlightlauncher.views

import androidx.core.content.ContextCompat
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.view.OptionMenu

internal class LauncherOptionMenu(
    private val menu: OptionMenu,
    private val delegate: Delegate,
) {
    interface Delegate {
        fun openSettings()

        fun openWidgetSelectorOverlay()

        fun enableWidgetEditMode()
    }

    init {
        with(menu) {
            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_vector_square),
                context.getString(R.string.launcher_option_menu_widgets_label),
            ) {
                delegate.openWidgetSelectorOverlay()
                menu.hide()
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_setting)!!,
                context.getString(R.string.launcher_option_menu_launcher_settings_label),
            ) {
                delegate.openSettings()
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_pen),
                context.getString(R.string.launcher_option_menu_edit_widgets_label),
            ) {
                delegate.enableWidgetEditMode()
                menu.hide()
            }
        }
    }
}
