package kenneth.app.starlightlauncher.extension

data class WidgetMetadata(
    /**
     * A unique string to identify the extension containing the widget.
     * For example, the package name of the extension can be used:
     * `com.my.extension`
     */
    val extensionName: String,

    /**
     * A user facing name of the widget
     */
    val displayName: String,

    /**
     * A user facing description that describes what this widget does
     */
    val description: String,
)