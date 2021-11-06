package kenneth.app.spotlightlauncher.prefs.datetime

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.location.component1
import androidx.core.location.component2
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.LatLong
import kenneth.app.spotlightlauncher.api.NominatimApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class WeatherLocationPickerFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var nominatimApi: NominatimApi

    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    @Inject
    lateinit var locationManager: LocationManager

    private var locationSearchBox: LocationSearchBoxPreference? = null

    private val apiScope = CoroutineScope(Dispatchers.IO)

    private val locationPermRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleLocationPermRequestResult
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.weather_location_preferences)
        changeToolbarTitle()

        locationSearchBox =
            findPreference<LocationSearchBoxPreference>(getString(R.string.date_time_location_search_box))
                ?.also { it.setSearchRequestListener(::performLocationSearch) }

        findPreference<Preference>(getString(R.string.date_time_use_current_location))?.setOnPreferenceClickListener {
            requestLocationPerm()
            HANDLED
        }
    }

    private fun requestLocationPerm() {
        locationPermRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun handleLocationPermRequestResult(isGranted: Boolean) {
        if (isGranted) {
            getCurrentLocation()
        }
    }

    /**
     * Searches for possible locations given a search query.
     */
    private fun performLocationSearch(query: String) {
        apiScope.launch {
            activity?.runOnUiThread {
                locationSearchBox?.isLoading = true
            }

            val places = nominatimApi.searchForLocations(query)
            places?.forEach { place ->
                preferenceScreen.addPreference(
                    Preference(context).apply {
                        title = place.displayName
                    }.also {
                        it.setOnPreferenceClickListener {
                            setWeatherLocation(LatLong(place.lat, place.long), place.displayName)
                            false
                        }
                    }
                )
            }

            activity?.runOnUiThread {
                locationSearchBox?.isLoading = false
            }
        }
    }

    private fun getCurrentLocation() {
        val context = this.context

        if (context != null && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationCriteria = Criteria().apply {
                accuracy = Criteria.ACCURACY_COARSE
                powerRequirement = Criteria.POWER_LOW
            }

            locationManager.getBestProvider(locationCriteria, true)
                ?.let { locationManager.getLastKnownLocation(it) }
                ?.let {
                    apiScope.launch {
                        val latLong = LatLong(it.latitude, it.longitude)
                        nominatimApi.reverseGeocode(latLong)?.let { place ->
                            setWeatherLocation(latLong, place.displayName)
                        } ?: activity?.runOnUiThread {
                            showLocationUnavailableDialog()
                        }
                    }
                }
                ?: showLocationUnavailableDialog()
        }
    }

    private fun setWeatherLocation(latLong: LatLong, displayName: String) {
        dateTimePreferenceManager.changeWeatherLocation(latLong, displayName)
        parentFragmentManager.popBackStack()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.date_time_location_picker_title)
    }

    private fun showLocationUnavailableDialog() {
        AlertDialog.Builder(context).run {
            setTitle(getString(R.string.date_time_current_location_unavailable_title))
            setMessage(getString(R.string.date_time_current_location_unavailable_summary))
            setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }
}