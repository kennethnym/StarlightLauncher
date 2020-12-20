package kenneth.app.spotlightlauncher.prefs.appearance

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_preferences, rootKey)

        findPreference<SwitchPreferenceCompat>(getString(R.string.appearance_show_app_labels))
            ?.isChecked = appearancePreferenceManager.showAppLabels
    }
}