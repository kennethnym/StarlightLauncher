package kenneth.app.spotlightlauncher

import android.util.DisplayMetrics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current state of the launcher.
 */
@Singleton
class AppState @Inject constructor() {
    enum class Theme {
        LIGHT, DARK
    }

    var screenWidth: Int = 0
    var screenHeight: Int = 0

    /**
     * Determines if MainActivity is starting for the first time.
     */
    var isInitialStart = true

    var theme: Theme = Theme.LIGHT

    val themeStyleId: Int
        get() = when (theme) {
            Theme.LIGHT -> R.style.LightLauncherTheme
            Theme.DARK -> R.style.DarkLauncherTheme
        }
}