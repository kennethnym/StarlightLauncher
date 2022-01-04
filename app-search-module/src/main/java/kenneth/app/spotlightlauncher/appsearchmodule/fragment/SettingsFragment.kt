package kenneth.app.spotlightlauncher.appsearchmodule.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.spotlightlauncher.appsearchmodule.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preference, rootKey)
    }
}