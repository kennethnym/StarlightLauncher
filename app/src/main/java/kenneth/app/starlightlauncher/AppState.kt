package kenneth.app.starlightlauncher

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current state of the launcher.
 */
@Singleton
internal class AppState @Inject constructor() {
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

    var theme: Theme = Theme.DARK

    var statusBarHeight = 0
}