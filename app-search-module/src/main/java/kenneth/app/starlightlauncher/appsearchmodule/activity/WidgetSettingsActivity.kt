package kenneth.app.starlightlauncher.appsearchmodule.activity

import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.appsearchmodule.fragment.WidgetSettingsFragment

class WidgetSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun createPreferenceFragment() = WidgetSettingsFragment()
}