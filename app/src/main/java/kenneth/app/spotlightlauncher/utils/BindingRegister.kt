package kenneth.app.spotlightlauncher.utils

import kenneth.app.spotlightlauncher.databinding.ActivityMainBinding
import kenneth.app.spotlightlauncher.databinding.SearchResultLayoutBinding
import kenneth.app.spotlightlauncher.searching.SearchResultView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class that stores references of view bindings of different views/activities
 */
object BindingRegister {
    /**
     * View binding of MainActivity
     */
    lateinit var activityMainBinding: ActivityMainBinding

    /**
     * View binding of search_result_layout, inflated by [SearchResultView].
     */
    lateinit var searchResultViewBinding: SearchResultLayoutBinding
}