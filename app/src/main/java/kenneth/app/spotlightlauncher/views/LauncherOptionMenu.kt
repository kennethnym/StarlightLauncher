package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.SettingsActivity

/**
 * Renders an option menu for configuring the launcher.
 * Can be triggered by long pressing the background, or by swiping up when scrolled to bottom.
 */
class LauncherOptionMenu(context: Context, attrs: AttributeSet) : BottomOptionMenu(context, attrs) {
    private val launcherSettingsItem: Item

    init {
        inflate(context, R.layout.launcher_option_menu, this)

        launcherSettingsItem = findViewById<Item>(R.id.launcher_settings_item)
            .also { it.setOnClickListener { openSettings() } }
    }

    private fun openSettings() {
        val settingsIntent = Intent(context, SettingsActivity::class.java)
        context.startActivity(settingsIntent)
        hide()
    }
}