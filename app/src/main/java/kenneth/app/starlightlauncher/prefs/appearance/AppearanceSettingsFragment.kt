package kenneth.app.starlightlauncher.prefs.appearance

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
internal class AppearanceSettingsFragment : PreferenceFragmentCompat(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_preferences, rootKey)

        changeToolbarTitle()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        activity?.let {
            // Instantiate the new Fragment
            val args = pref.extras
            val fragment = childFragmentManager.fragmentFactory.instantiate(
                it.classLoader,
                pref.fragment
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }

            // Replace the existing Fragment with the new Fragment
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .addToBackStack(null)
                .commit()
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        changeToolbarTitle()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.appearance_title)
    }
}