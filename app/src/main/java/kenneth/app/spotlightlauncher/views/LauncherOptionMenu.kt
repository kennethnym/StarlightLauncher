package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.view.OptionMenu
import kenneth.app.spotlightlauncher.databinding.LauncherOptionMenuBinding
import kenneth.app.spotlightlauncher.prefs.SettingsActivity

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
