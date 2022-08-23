package kenneth.app.starlightlauncher

import kenneth.app.starlightlauncher.databinding.ActivityMainBinding
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding
import kenneth.app.starlightlauncher.databinding.WidgetsPanelBinding
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class that stores references of view bindings of different views/activities
 */
@Singleton
internal class BindingRegister @Inject constructor() {
    /**
     * View binding of MainActivity
     */
    lateinit var activityMainBinding: ActivityMainBinding

    lateinit var mainScreenBinding: FragmentMainScreenBinding

    /**
     * View binding of widgets panel
     */
    lateinit var widgetsPanelBinding: WidgetsPanelBinding
}