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
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kenneth.app.starlightlauncher.datetime.DateTimePreferenceManager
import kenneth.app.starlightlauncher.datetime.DateTimeViewSize
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceManager
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceChanged
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
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
    private val widgetPreferenceManager: WidgetPreferenceManager,
    private val searchPreferenceManager: SearchPreferenceManager,
    private val openWeatherApi: OpenWeatherApi,
    private val launcherEventChannel: LauncherEventChannel,
    private val searcher: Searcher,
) : ViewModel(), LocationListener {
    private val weatherCheckTimer = Timer()

    private var weatherCheckTimerTask: WeatherCheckTimerTask? = null

    /**
     * A coroutine scope whose lifecycle is bound to whether
     * weather is enabled. When the user turns off weather, this scope will be canceled.
     * This scope will also be canceled when this [ViewModel] is cleared.
     */
    private val weatherScope = MainScope()

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

    private val _addedAndroidWidget by lazy {
        MutableLiveData<AddedWidget.AndroidWidget>()
    }

    /**
     * The most recently added android widget.
     */
    val addedAndroidWidget: LiveData<AddedWidget.AndroidWidget> = _addedAndroidWidget

    private val _addedWidgets by lazy {
        MutableLiveData<List<AddedWidget>>()
    }

    val addedWidgets: LiveData<List<AddedWidget>> = _addedWidgets

    private val _clockSize by lazy {
        MutableLiveData<DateTimeViewSize>()
    }

    /**
     * The size of the clock.
     */
    val clockSize: LiveData<DateTimeViewSize> = _clockSize

    private val _shouldUse24HrClock by lazy {
        MutableLiveData<Boolean>()
    }

    val shouldUse24HrClock: LiveData<Boolean> = _shouldUse24HrClock

    private val _searchResultOrder by lazy {
        MutableLiveData<List<String>>()
    }

    val searchResultOrder: LiveData<List<String>> = _searchResultOrder

    private val locationManager = context.getSystemService(LocationManager::class.java)

    /**
     * Current location of the device. Only used for retrieving weather info
     * at that location. null if automatic location detection is disabled,
     * or if it is not available.
     */
    private var currentDeviceLocation: Location? = null

    init {
        _addedWidgets.postValue(widgetPreferenceManager.addedWidgets)

        with(viewModelScope) {
            launch {
                val shouldShowWeather = dateTimePreferenceManager.shouldShowWeather.first()
                if (shouldShowWeather) {
                    initializeWeather()
                    attachWeatherSettingsListeners()
                } else {
                    weatherScope.cancel()
                    _weatherInfo.postValue(null)
                }
            }

            launch {
                launcherEventChannel.subscribe {
                    when (it) {
                        is WidgetPreferenceChanged.NewAndroidWidgetAdded -> {
                            _addedAndroidWidget.postValue(it.addedWidget)
                        }

                        is WidgetPreferenceChanged.WidgetRemoved -> {
                            _addedWidgets.postValue(widgetPreferenceManager.addedWidgets)
                        }
                    }
                }
            }

            launch {
                searchPreferenceManager.searchModuleOrder.collectLatest {
                    _searchResultOrder.postValue(it)
                }
            }
        }
    }

    override fun onCleared() {
        weatherCheckTimer.cancel()
        weatherScope.cancel()
    }

    override fun onLocationChanged(location: Location) {
        currentDeviceLocation = location
    }

    fun requestSearch(query: String) {
        searcher.requestSearch(query)
    }

    fun cancelPendingSearch() {
        searcher.cancelPendingSearch()
    }

    fun refreshWeather() {
        weatherScope.launch {
            bestWeatherLocation()?.let {
                loadWeather(it)
            }
        }
    }

    fun refreshWidgetList() {
        _addedWidgets.postValue(widgetPreferenceManager.addedWidgets)
    }

    fun removeAndroidWidget(appWidgetId: Int) {
        viewModelScope.launch {
            widgetPreferenceManager.removeAndroidWidget(appWidgetId)
            refreshWidgetList()
        }
    }

    fun swapWidget(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            widgetPreferenceManager.changeWidgetOrder(fromPosition, toPosition)
        }
    }

    fun resizeWidget(widget: AddedWidget, newHeight: Int) {
        viewModelScope.launch {
            widgetPreferenceManager.changeWidgetHeight(widget, newHeight)
        }
    }

    fun removeWidget(widget: AddedWidget) {
        when (widget) {
            is AddedWidget.AndroidWidget -> viewModelScope.launch {
                widgetPreferenceManager.removeAndroidWidget(widget.appWidgetId)
                _addedWidgets.postValue(widgetPreferenceManager.addedWidgets)
            }

            is AddedWidget.StarlightWidget -> viewModelScope.launch {
                widgetPreferenceManager.removeStarlightWidget(widget.extensionName)
                _addedWidgets.postValue(widgetPreferenceManager.addedWidgets)
            }
        }
    }

    /**
     * The best location to be used for fetching weather.
     *
     * If user enables auto location, the last known device location is used.
     * Otherwise, this falls back to the location set by the user.
     */
    private suspend fun bestWeatherLocation() =
        currentDeviceLocation
            ?.let { LatLong(it) }
            ?: dateTimePreferenceManager.weatherLocation.first()

    private suspend fun initializeWeather() {
        val shouldUseAutoLocation = dateTimePreferenceManager.shouldUseAutoWeatherLocation.first()
        if (shouldUseAutoLocation && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // if auto weather location is enabled
            // first request the initial device location
            // then request for subsequent location update
            // because the request might not immediately call the listener (which is this fragment)
            requestLocationUpdate()
        } else {
            scheduleWeatherUpdate()
        }
    }

    /**
     * Listen to weather-related settings.
     * The listeners will update the weather info according to the latest weather settings.
     */
    private fun attachWeatherSettingsListeners() {
        with(weatherScope) {
            launch {
                dateTimePreferenceManager.weatherLocation.collectLatest {
                    if (it != null) {
                        loadWeather(location = it)
                    }
                }
            }

            launch {
                dateTimePreferenceManager.shouldShowWeather.collectLatest { shouldShowWeather ->
                    if (shouldShowWeather) {
                        initializeWeather()
                    } else {
                        currentDeviceLocation = null
                        locationManager.removeUpdates(this@MainScreenViewModel)
                    }
                }
            }

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
            }

            launch {
                dateTimePreferenceManager.weatherCheckFrequency.collectLatest { frequency ->
                    weatherCheckTimerTask?.cancel()
                    scheduleWeatherUpdate(frequency)
                }
            }

            launch {
                dateTimePreferenceManager.locationCheckFrequency.collectLatest { frequency ->
                    requestLocationUpdate(frequency)
                }
            }

            launch {
                dateTimePreferenceManager.weatherUnit.collectLatest { weatherUnit ->
                    bestWeatherLocation()?.let {
                        loadWeather(
                            location = it,
                            weatherUnit,
                        )
                    }
                }
            }

            launch {
                dateTimePreferenceManager.dateTimeViewSize.collectLatest {
                    _clockSize.postValue(it)
                }
            }

            launch {
                dateTimePreferenceManager.shouldUse24HrClock.collectLatest {
                    _shouldUse24HrClock.postValue(it)
                }
            }
        }
    }

    private suspend fun requestLocationUpdate(frequency: Long? = null) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // no permission to access location
            return
        }

        val locationCheckFrequency =
            frequency ?: dateTimePreferenceManager.locationCheckFrequency.first()

        locationManager?.getBestProvider(weatherLocationCriteria, true)?.let { provider ->
            // get the last known location
            val lastLocation = locationManager.getLastKnownLocation(provider)
            // record the last known location
            currentDeviceLocation = lastLocation

            // schedule weather update if last known location is available
            lastLocation?.let {
                scheduleWeatherUpdate()
            }

            locationManager.requestLocationUpdates(
                provider,
                locationCheckFrequency,
                1000F,
                this
            )
        }
    }

    private suspend fun scheduleWeatherUpdate(frequency: Long? = null) {
        val updateFrequency = frequency ?: dateTimePreferenceManager.weatherCheckFrequency.first()
        weatherCheckTimer.scheduleAtFixedRate(
            WeatherCheckTimerTask().also { weatherCheckTimerTask = it },
            0,
            updateFrequency
        )
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

    private inner class WeatherCheckTimerTask : TimerTask() {
        override fun run() {
            weatherScope.launch {
                bestWeatherLocation()?.let {
                    loadWeather(it)
                }
            }
        }
    }
}
