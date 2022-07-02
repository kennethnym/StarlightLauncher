package kenneth.app.starlightlauncher.prefs.datetime

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.NominatimApi
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
internal class WeatherLocationPickerFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var nominatimApi: NominatimApi

    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Named(IO_DISPATCHER)
    lateinit var ioDispatcher: CoroutineDispatcher

    private var locationSearchBox: LocationSearchBoxPreference? = null

    private val locationPermRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleLocationPermRequestResult
    )

    private val addedLocationPreferences = mutableListOf<Preference>()

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
        CoroutineScope(mainDispatcher).launch {
            locationSearchBox?.isLoading = true
            val places = withContext(ioDispatcher) {
                nominatimApi.searchForLocations(query)
            }
            with(addedLocationPreferences) {
                forEach { preferenceScreen.removePreference(it) }
                clear()
            }
            places.getOrNull()?.forEach { place ->
                context?.let { context ->
                    preferenceScreen.addPreference(
                        Preference(context).apply {
                            title = place.displayName
                        }.also {
                            addedLocationPreferences += it
                            it.setOnPreferenceClickListener {
                                setWeatherLocation(
                                    LatLong(place.lat, place.long),
                                    place.displayName
                                )
                                false
                            }
                        }
                    )
                }
            }
            locationSearchBox?.isLoading = false
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
                    CoroutineScope(mainDispatcher).launch {
                        val latLong = LatLong(it)
                        withContext(ioDispatcher) {
                            nominatimApi.reverseGeocode(latLong)
                        }.getOrNull()?.let { result ->
                            setWeatherLocation(latLong, result.displayName)
                        } ?: showLocationUnavailableDialog()
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