package kenneth.app.starlightlauncher.prefs.searchlayout

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.spotlightlauncher.R

class SearchLayoutSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.search_layout_preferences, rootKey)
    }
}