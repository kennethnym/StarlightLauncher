package kenneth.app.starlightlauncher

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.home.MainScreenFragment
import kenneth.app.starlightlauncher.datetime.DateTimePreferenceManager
import javax.inject.Inject

internal class LauncherFragmentFactory @Inject constructor(
    private val bindingRegister: BindingRegister,
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (className) {
            MainScreenFragment::class.java.name -> MainScreenFragment(bindingRegister)
            else -> super.instantiate(classLoader, className)
        }
}