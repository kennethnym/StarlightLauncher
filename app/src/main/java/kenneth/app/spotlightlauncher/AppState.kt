package kenneth.app.spotlightlauncher

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
        set(height) {
            field = height
            halfScreenHeight = height / 2
        }

    var halfScreenHeight: Int = 0
        private set

    /**
     * Determines if MainActivity is starting for the first time.
     */
    var isInitialStart = true

    var theme: Theme = Theme.DARK

    val themeStyleId: Int
        get() = when (theme) {
            Theme.LIGHT -> R.style.LightLauncherTheme
            Theme.DARK -> R.style.DarkLauncherTheme
        }

    /**
     * Current background color of various views on the home screen based on
     * the dominant color of the wallpaper.
     */
    var adaptiveBackgroundColor: Int = 0

    /**
     * Current text color that changes based on [AppState.adaptiveBackgroundColor]
     * to make sure there is enough contrast between the content and the backgound.
     */
    var adaptiveTextColor: Int = 0

    /**
     * Determines if the search box is activated.
     */
    var isSearchBoxActive: Boolean = false

    var statusBarHeight = 0

    var isWidgetPanelExpanded = false
}