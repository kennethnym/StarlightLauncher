package kenneth.app.spotlightlauncher.extension

import kenneth.app.spotlightlauncher.api.SearchModule
import kenneth.app.spotlightlauncher.api.WidgetCreator

data class Extension(
    val packageName: String,
    val searchModule: SearchModule? = null,
    val widget: WidgetCreator? = null,
) {
}
