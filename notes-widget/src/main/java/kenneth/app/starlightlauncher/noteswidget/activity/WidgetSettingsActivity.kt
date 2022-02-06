package kenneth.app.starlightlauncher.noteswidget.activity

import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.noteswidget.fragment.WidgetSettingsFragment

class WidgetSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun createPreferenceFragment() = WidgetSettingsFragment()
}