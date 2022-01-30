package kenneth.app.starlightlauncher.api

import android.view.View
import android.view.ViewGroup

interface WidgetCreator {
    fun createWidget(parent: ViewGroup): WidgetHolder
}

interface WidgetHolder {
    val rootView: View
}
