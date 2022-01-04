package kenneth.app.spotlightlauncher.api.theme

/**
 * Describes the theme of Spotlight Launcher that is adaptive to the current wallpaper.
 */
data class AdaptiveTheme(
    /**
     * Current background color of various views on the home screen based on
     * the dominant color of the wallpaper.
     */
    val adaptiveBackgroundColor: Int = 0,

    /**
     * Current text color that changes based on [AdaptiveTheme.adaptiveBackgroundColor]
     * to make sure there is enough contrast between the content and the backgound.
     */
    val adaptiveTextColor: Int = 0
)