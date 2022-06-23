package kenneth.app.starlightlauncher.prefs

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.R

class AboutLauncherSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about_launcher_preferences, rootKey)

        findPreference<Preference>(getString(R.string.pref_key_launcher_version))
            ?.summary = BuildConfig.VERSION_NAME
    }
}