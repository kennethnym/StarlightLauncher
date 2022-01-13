package kenneth.app.spotlightlauncher.extension

import kenneth.app.spotlightlauncher.api.SearchModule

interface ExtensionMetadata {
    /**
     * The name of the package that contains this extension.
     */
    val packageName: String

    /**
     * A unique string to identify the entry class of the extension..
     * For example, the full package path to this module can be used:
     * `com.my.package.MyExtension`
     */
    val name: String

    /**
     * A user facing name of this [SearchModule]
     */
    val displayName: String

    /**
     * A user facing description that describes what this [SearchModule] does
     */
    val description: String
}