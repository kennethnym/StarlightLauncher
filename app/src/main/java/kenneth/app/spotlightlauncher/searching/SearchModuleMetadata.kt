package kenneth.app.spotlightlauncher.searching

import android.content.ComponentName
import kenneth.app.spotlightlauncher.api.SearchModule

data class SearchModuleMetadata(
    /**
     * A unique string to identify this search module.
     * For example, the full package path to this module can be used:
     * `com.my.package.MySearchModule`
     */
    val name: String,

    /**
     * A user facing name of this [SearchModule]
     */
    val displayName: String,

    /**
     * A user facing description that describes what this [SearchModule] does
     */
    val description: String,

    /**
     * The [ComponentName] that corresponds to the settings activity of the search module.
     *
     * This is null when the search module doesn't have a settings activity.
     */
    val settingsActivityComponent: ComponentName? = null
)