package kenneth.app.starlightlauncher.prefs

import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject

@AndroidEntryPoint
class StarlightLauncherSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    internal lateinit var extensionManager: ExtensionManager

    override fun createPreferenceFragment(): PreferenceFragmentCompat = RootSettingsFragment()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }
}