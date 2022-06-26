package kenneth.app.starlightlauncher.prefs.datetime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
internal class DateTimeSettingsFragment : PreferenceFragmentCompat(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    private lateinit var use24HrClockPref: SwitchPreference
    private lateinit var showWeatherPref: SwitchPreference
    private lateinit var useAutoWeatherLocationPref: SwitchPreference
    private lateinit var weatherLocationPickerPref: Preference

    private val locationPermRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleLocationPermRequestResult
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.date_time_preferences, rootKey)

        showWeatherPref = findPreference(getString(R.string.date_time_show_weather))!!
        weatherLocationPickerPref = findPreference(getString(R.string.date_time_location_picker))!!
        useAutoWeatherLocationPref =
            findPreference(getString(R.string.date_time_use_auto_location))!!
        use24HrClockPref = findPreference(getString(R.string.date_time_use_24hr_clock))!!

        showWeatherPref.setOnPreferenceChangeListener { pref, value ->
            if (value is Boolean) {
                useAutoWeatherLocationPref.isEnabled = value
                weatherLocationPickerPref.isEnabled = value
            }
            true
        }

        useAutoWeatherLocationPref.run {
            setOnPreferenceClickListener { pref ->
                if (pref is SwitchPreference) {
                    if (context?.let {
                            ActivityCompat.checkSelfPermission(
                                it,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        } != PackageManager.PERMISSION_GRANTED
                    ) {
                        useAutoWeatherLocationPref.isChecked = false
                        locationPermRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
                HANDLED
            }

            setOnPreferenceChangeListener { _, value ->
                if (value is Boolean) {
                    weatherLocationPickerPref.isEnabled = !value
                    HANDLED
                } else false
            }
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
        showCurrentPreferences()
    }

    private fun handleLocationPermRequestResult(isGranted: Boolean) {
        useAutoWeatherLocationPref.isChecked = isGranted
    }

    private fun showCurrentPreferences() {
        showPickedLocation()
        val shouldShowWeather = dateTimePreferenceManager.shouldShowWeather
        use24HrClockPref.isChecked = dateTimePreferenceManager.shouldUse24HrClock
        useAutoWeatherLocationPref.isChecked =
            dateTimePreferenceManager.shouldUseAutoWeatherLocation
        showWeatherPref.isChecked = shouldShowWeather
        weatherLocationPickerPref.isEnabled =
            shouldShowWeather && !useAutoWeatherLocationPref.isChecked
        useAutoWeatherLocationPref.isEnabled = shouldShowWeather
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.date_time_pref_title)
    }

    private fun showPickedLocation() {
        weatherLocationPickerPref.apply {
            val pickedLocation = dateTimePreferenceManager.weatherLocationName
            summary =
                """${getString(R.string.date_time_pick_location_summary)}
${getString(R.string.date_time_picked_location_label, pickedLocation)}"""
        }
    }
}