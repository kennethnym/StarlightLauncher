package kenneth.app.spotlightlauncher.prefs.datetime

import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.LatLong
import kenneth.app.spotlightlauncher.api.NominatimApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WeatherLocationPickerFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var nominatimApi: NominatimApi

    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    private var locationSearchBox: LocationSearchBoxPreference? = null

    private val apiScope = CoroutineScope(Dispatchers.IO)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.weather_location_preferences)

        locationSearchBox =
            findPreference<LocationSearchBoxPreference>(getString(R.string.date_time_location_search_box))
                ?.also { it.setSearchRequestListener(::performLocationSearch) }

        changeToolbarTitle()
    }

    /**
     * Searches for possible locations given a search query.
     */
    private fun performLocationSearch(query: String) {
        apiScope.launch {
            activity?.runOnUiThread {
                locationSearchBox?.isLoading = true
            }

            nominatimApi.searchForLocations(query)

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

    private fun setWeatherLocation(latLong: LatLong, displayName: String) {
        dateTimePreferenceManager.changeWeatherLocation(latLong, displayName)
        parentFragmentManager.popBackStack()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.date_time_location_picker_title)
    }
}