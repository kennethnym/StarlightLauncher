package kenneth.app.starlightlauncher.views

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.api.view.OptionMenuItem
import kenneth.app.starlightlauncher.prefs.StarlightLauncherSettingsActivity
import kenneth.app.starlightlauncher.utils.BindingRegister
import kenneth.app.starlightlauncher.widgets.availablewidgetspage.AvailableWidgetsPage

class LauncherOptionMenu(
    private val context: Context,
    private val launcher: StarlightLauncherApi,
    private val menu: OptionMenu,
) {
    init {
        with(menu) {
            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_vector_square),
                context.getString(R.string.launcher_option_menu_widgets_label),
            ) {
                menu.hide()
                launcher.showOverlay(it, ::AvailableWidgetsPage)
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_setting)!!,
                context.getString(R.string.launcher_option_menu_launcher_settings_label),
            ) {
                openSettingsActivity()
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_unlock_alt),
                context.getString(R.string.launcher_option_menu_unlock_widgets_label),
            ) {
                toggleWidgetLock(it)
            }
        }
    }

    private fun openSettingsActivity() {
        context.startActivity(
            Intent(context, StarlightLauncherSettingsActivity::class.java)
        )
    }

    /**
     * Toggle widget lock. If widgets are locked, unlock them, vice versa.
     */
    private fun toggleWidgetLock(menuItem: OptionMenuItem) {
        with(BindingRegister.widgetsPanelBinding.widgetList) {
            if (areWidgetsLocked) {
                unlockWidgets()
                menuItem.apply {
                    itemLabel = context.getString(R.string.launcher_option_menu_lock_widgets_label)
                    itemIcon = ContextCompat.getDrawable(context, R.drawable.ic_lock_alt)
                }
            } else {
                lockWidgets()
                menuItem.apply {
                    itemLabel =
                        context.getString(R.string.launcher_option_menu_unlock_widgets_label)
                    itemIcon = ContextCompat.getDrawable(context, R.drawable.ic_unlock_alt)
                }
            }
        }
    }
}
