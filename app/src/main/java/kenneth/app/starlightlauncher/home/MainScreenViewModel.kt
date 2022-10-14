package kenneth.app.starlightlauncher.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kenneth.app.starlightlauncher.datetime.DateTimePreferenceManager
import kenneth.app.starlightlauncher.datetime.DateTimeViewSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val weatherLocationCriteria = Criteria().apply {
    accuracy = Criteria.ACCURACY_COARSE
    powerRequirement = Criteria.POWER_LOW
}

@HiltViewModel
// the context object being injected here will not leak because it is an application context
// which is unique across the entire application
// so it is safe to ignore the lint warning
// see:
// https://stackoverflow.com/questions/68371219/this-field-leaks-a-context-object-warning-hilt-injection
// https://stackoverflow.com/questions/66216839/inject-context-with-hilt-this-field-leaks-a-context-object
@SuppressLint("StaticFieldLeak")
internal class MainScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dateTimePreferenceManager: DateTimePreferenceManager,
    private val openWeatherApi: OpenWeatherApi,
) : ViewModel(), LocationListener {
    private val _weatherInfo by lazy {
        MutableLiveData<Pair<TemperatureUnit, OpenWeatherApi.Response>?>()
    }

    /**
     * A live data of the latest weather info as a [Pair].
     * The first item is the [TemperatureUnit] the weather info is in,
     * and the second item contains the actual weather info.
     * This is updated at the frequency that the user set.
     * By default, the weather is updated every hour.
     *
     * If this returns null, then weather info is unavailable.
     */
    val weatherInfo: LiveData<Pair<TemperatureUnit, OpenWeatherApi.Response>?> = _weatherInfo

    private val _clockSize by lazy {
        MutableLiveData<DateTimeViewSize>()
    }

    /**
     * The size of the clock.
     */
    val clockSize: LiveData<DateTimeViewSize> = _clockSize

    private val locationManager = context.getSystemService(LocationManager::class.java)

    /**
     * Current location of the device. Only used for retrieving weather info
     * at that location. null if automatic location detection is disabled,
     * or if it is not available.
     */
    private var currentDeviceLocation: Location? = null

    /**
     * Jobs created to handle weather feature.
     * When user turns off "show weather", all the jobs are canceled.
     */
    private var weatherRelatedJobs = mutableListOf<Job>()

    init {
        with(viewModelScope) {
            launch {
                val shouldShowWeather = dateTimePreferenceManager.shouldShowWeather.first()
                if (shouldShowWeather) {
                    initializeWeather()
                    attachWeatherSettingsListeners()
                } else {
                    weatherRelatedJobs.forEach { it.cancel() }
                    _weatherInfo.postValue(null)
                }
            }

            launch {
                dateTimePreferenceManager.dateTimeViewSize.collectLatest {
                    _clockSize.postValue(it)
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentDeviceLocation = location
    }

    private suspend fun initializeWeather() {
        val shouldUseAutoLocation = dateTimePreferenceManager.shouldUseAutoWeatherLocation.first()
        if (shouldUseAutoLocation && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // if auto weather location is enabled
            // first request the initial device location
            // then request for subsequent location update
            // because the request might not immediately call the listener (which is this fragment)
            requestLocationUpdate()
        } else {
            dateTimePreferenceManager.weatherLocation.first()?.let {
                loadWeather(location = it)
            }
        }
    }

    /**
     * Listen to weather-related settings.
     * The listeners will update the weather info according to the latest weather settings.
     */
    private suspend fun attachWeatherSettingsListeners() {
        with(viewModelScope) {
            launch {
                dateTimePreferenceManager.weatherLocation.collectLatest {
                    if (it != null) {
                        loadWeather(location = it)
                    }
                }
            }.also { weatherRelatedJobs += it }

            launch {
                dateTimePreferenceManager.shouldShowWeather.collectLatest { shouldShowWeather ->
                    if (shouldShowWeather) {
                        initializeWeather()
                    } else {
                        currentDeviceLocation = null
                        locationManager.removeUpdates(this@MainScreenViewModel)
                    }
                }
            }.also { weatherRelatedJobs += it }

            launch {
                dateTimePreferenceManager.shouldUseAutoWeatherLocation.collectLatest {
                    if (it) {
                        // auto location enabled, request for location update
                        requestLocationUpdate()
                    } else {
                        // auto location disabled, erase previously saved location
                        currentDeviceLocation = null
                        // check if there's a fallback weather location that user has set
                        // if so, load weather for the location
                        // otherwise, the launcher doesn't know what location to show weather for
                        dateTimePreferenceManager.weatherLocation.first()?.let { location ->
                            // user manually set a weather location
                            // load weather info at that location
                            loadWeather(location)
                        } ?: kotlin.run {
                            _weatherInfo.postValue(null)
                        }
                    }
                }
            }.also { weatherRelatedJobs += it }

            launch {
                dateTimePreferenceManager.weatherCheckFrequency.collectLatest { frequency ->
                    requestLocationUpdate(frequency)
                }
            }.also { weatherRelatedJobs += it }

            launch {
                dateTimePreferenceManager.weatherUnit.collectLatest { weatherUnit ->
                    val weatherLocation = currentDeviceLocation?.let { LatLong(it) }
                        ?: dateTimePreferenceManager.weatherLocation.first()
                    weatherLocation?.let {
                        loadWeather(
                            location = it,
                            weatherUnit,
                        )
                    }
                }
            }.also { weatherRelatedJobs += it }
        }
    }

    private suspend fun requestLocationUpdate(frequency: Long? = null) {
        val locationCheckFrequency =
            frequency ?: dateTimePreferenceManager.weatherCheckFrequency.first()

        locationManager?.getBestProvider(weatherLocationCriteria, true)?.let { provider ->
            // get the last known location
            val lastLocation = locationManager.getLastKnownLocation(provider)
            // record the last known location
            currentDeviceLocation = lastLocation
            // load and show weather info if last known location is available
            lastLocation?.let { loadWeather(location = LatLong(it)) }

            locationManager.requestLocationUpdates(
                provider,
                locationCheckFrequency,
                1000F,
                this
            )
        }
    }

    private suspend fun loadWeather(location: LatLong, weatherUnit: TemperatureUnit? = null) {
        openWeatherApi
            .run {
                latLong = location
                unit = weatherUnit ?: dateTimePreferenceManager.weatherUnit.first()
                getCurrentWeather()
            }
            .getOrNull()
            ?.let {
                _weatherInfo.postValue(
                    Pair(openWeatherApi.unit, it)
                )
            }
            ?: _weatherInfo.postValue(null)
    }
}
