package kenneth.app.starlightlauncher.filesearchmodule.activity

import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.filesearchmodule.fragment.SearchModuleSettingsFragment

class SearchModuleSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun createPreferenceFragment() = SearchModuleSettingsFragment()
}