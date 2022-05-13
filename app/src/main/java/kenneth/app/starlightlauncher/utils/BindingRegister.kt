package kenneth.app.starlightlauncher.utils

import kenneth.app.starlightlauncher.databinding.ActivityMainBinding
import kenneth.app.starlightlauncher.databinding.WidgetsPanelBinding

/**
 * A class that stores references of view bindings of different views/activities
 */
internal object BindingRegister {
    /**
     * View binding of MainActivity
     */
    lateinit var activityMainBinding: ActivityMainBinding

    /**
     * View binding of widgets panel
     */
    lateinit var widgetsPanelBinding: WidgetsPanelBinding
}