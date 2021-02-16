package kenneth.app.spotlightlauncher.prefs.datetime

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
class DateTimeSettingsFragment : PreferenceFragmentCompat(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.date_time_preferences, rootKey)
        changeToolbarTitle()

        findPreference<Preference>(getString(R.string.date_time_location_picker))?.apply {
            val pickedLocation = dateTimePreferenceManager.weatherLocationName
            summary =
                "$summary\n${getString(R.string.date_time_picked_location_label, pickedLocation)}"
        }

        findPreference<SwitchPreferenceCompat>(getString(R.string.date_time_use_24hr_clock))?.apply {
            isChecked = dateTimePreferenceManager.shouldUse24HrClock
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        activity?.let {
            // Instantiate the new Fragment
            val args = pref.extras
            val fragment = childFragmentManager.fragmentFactory.instantiate(
                it.classLoader,
                pref.fragment
            ).apply {
                arguments = args
                setTargetFragment(caller, 0)
            }

            // Replace the existing Fragment with the new Fragment
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .addToBackStack(null)
                .commit()
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        changeToolbarTitle()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.date_time_pref_title)
    }
}