package kenneth.app.starlightlauncher.extension

import kenneth.app.starlightlauncher.api.ExtensionSettingsProvider
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.WidgetCreator

/**
 * Describes an extension of Starlight Launcher.
 */
data class Extension(
    /**
     * A unique name that identifies this extension. This should be the full name
     * of the package that exports the extension points, i.e. search module and widget.
     */
    val name: String,

    /**
     * The [SearchModule] exported by this extension.
     * This is null if this extension doesn't export one.
     */
    val searchModule: SearchModule? = null,

    /**
     * The Starlight widget exported by this extension.
     * This is null if this extension doesn't export one.
     */
    val widget: WidgetCreator? = null,

    /**
     * Optional settings provided by this extension.
     * An entry will be added in the settings which will provide
     * access to the settings screen provided by this [Extension].
     */
    val settingsProvider: ExtensionSettingsProvider? = null
)
