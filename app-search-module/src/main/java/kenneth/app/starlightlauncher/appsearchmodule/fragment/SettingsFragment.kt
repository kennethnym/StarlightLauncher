package kenneth.app.starlightlauncher.appsearchmodule.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.appsearchmodule.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preference, rootKey)
    }
}