package kenneth.app.spotlightlauncher

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class FileSearchSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.file_search_preferences, rootKey)
    }
}
