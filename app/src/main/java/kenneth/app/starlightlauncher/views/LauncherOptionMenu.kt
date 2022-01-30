package kenneth.app.starlightlauncher.views

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.prefs.SettingsActivity

fun buildLauncherOptionMenu(menu: OptionMenu, context: Context) {
    menu.addItem(
        ContextCompat.getDrawable(context, R.drawable.ic_setting)!!,
        context.getString(R.string.launcher_settings_label),
    ) {
        openSettingsActivity(context)
    }
}

private fun openSettingsActivity(context: Context) {
    context.startActivity(
        Intent(context, SettingsActivity::class.java)
    )
}
