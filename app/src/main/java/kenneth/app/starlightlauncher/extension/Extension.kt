package kenneth.app.starlightlauncher.extension

import android.content.Intent
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.WidgetCreator

data class Extension(
    val name: String,
    val searchModule: SearchModule? = null,
    val widget: WidgetCreator? = null,
)