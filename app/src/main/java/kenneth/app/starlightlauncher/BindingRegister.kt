package kenneth.app.starlightlauncher

import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding
import javax.inject.Inject

/**
 * A class that stores references of view bindings of different views/activities
 */
@ActivityScoped
internal class BindingRegister @Inject constructor() {
    lateinit var mainScreenBinding: FragmentMainScreenBinding
}