package kenneth.app.starlightlauncher.views

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.prefs.StarlightLauncherSettingsActivity
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.widgets.availablewidgetspage.AvailableWidgetsFragment

internal class LauncherOptionMenu(
    private val context: Context,
    private val launcher: StarlightLauncherApi,
    private val bindingRegister: BindingRegister,
    private val menu: OptionMenu,
) {
    init {
        with(menu) {
            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_vector_square),
                context.getString(R.string.launcher_option_menu_widgets_label),
            ) {
                menu.hide()
                launcher.showOverlay(AvailableWidgetsFragment())
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_setting)!!,
                context.getString(R.string.launcher_option_menu_launcher_settings_label),
            ) {
                openSettingsActivity()
            }

            addItem(
                ContextCompat.getDrawable(context, R.drawable.ic_pen),
                context.getString(R.string.launcher_option_menu_edit_widgets_label),
            ) {
                enableWidgetEditMode()
                menu.hide()
            }
        }
    }

    private fun openSettingsActivity() {
        context.startActivity(
            Intent(context, StarlightLauncherSettingsActivity::class.java)
        )
    }

    private fun enableWidgetEditMode() {
        bindingRegister.mainScreenBinding.widgetsPanel.editWidgets()
    }
}
