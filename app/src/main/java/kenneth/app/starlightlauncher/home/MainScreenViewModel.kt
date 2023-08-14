package kenneth.app.starlightlauncher.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kenneth.app.starlightlauncher.datetime.DateTimePreferenceManager
import kenneth.app.starlightlauncher.datetime.DateTimeViewSize
import kenneth.app.starlightlauncher.mediacontrol.NotificationListenerStub
import kenneth.app.starlightlauncher.mediacontrol.settings.MediaControlPreferenceManager
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceManager
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    private val appearancePreferenceManager: AppearancePreferenceManager,
    private val dateTimePreferenceManager: DateTimePreferenceManager,
    private val widgetPreferenceManager: WidgetPreferenceManager,
    private val searchPreferenceManager: SearchPreferenceManager,
    private val mediaControlPreferenceManager: MediaControlPreferenceManager,
    private val openWeatherApi: OpenWeatherApi,
    private val searcher: Searcher,
    private val mediaSessionManager: MediaSessionManager,
) : ViewModel(),
    LocationListener,
    MediaSessionManager.OnActiveSessionsChangedListener {
    private val weatherCheckTimer = Timer()

    /**
     * The ComponentName of the notification listener stub
     */
    private val notificationListenerStubComponent =
        ComponentName(context, NotificationListenerStub::class.java)

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

    private val _isMediaControlEnabled by lazy {
        MutableLiveData<Boolean>()
    }

    /**
     * Whether the media control widget is enabled.
     */
    val isMediaControlEnabled: LiveData<Boolean> = _isMediaControlEnabled

    private val _shouldUse24HrClock by lazy {
        MutableLiveData<Boolean>()
    }

    val shouldUse24HrClock: LiveData<Boolean> = _shouldUse24HrClock

    private val _searchResultOrder by lazy {
        MutableLiveData<List<String>>()
    }

    val searchResultOrder: LiveData<List<String>> = _searchResultOrder

    private val _activeMediaSession by lazy {
        MutableLiveData<MediaController?>()
    }

    val activeMediaSession: LiveData<MediaController?> = _activeMediaSession

    private val isNotificationListenerEnabled by lazy {
        MutableLiveData(isNotificationListenerEnabled())
    }

    private val _shouldMediaControlBeVisible by lazy {
        MediatorLiveData<Boolean>()
    }

    val shouldMediaControlBeVisible: LiveData<Boolean> = _shouldMediaControlBeVisible

    private val _isAllAppsScreenEnabled by lazy {
        MutableLiveData<Boolean>()
    }

    val isAllAppsScreenEnabled: LiveData<Boolean> = _isAllAppsScreenEnabled

    private val locationManager = context.getSystemService(LocationManager::class.java)

    /**
     * Current location of the device. Only used for retrieving weather info
     * at that location. null if automatic location detection is disabled,
     * or if it is not available.
     */
    private var currentDeviceLocation: Location? = null

    private var isLoadingWeather = false

    private var weatherApiKey = BuildConfig.OPEN_WEATHER_API_KEY

    init {
        if (isNotificationListenerEnabled.value == true) {
            observeActiveMediaSession()
        }

        with(_shouldMediaControlBeVisible) {
            addSource(isMediaControlEnabled) {
                _shouldMediaControlBeVisible.postValue(it && _activeMediaSession.value != null && isNotificationListenerEnabled.value == true)
            }
            addSource(isNotificationListenerEnabled) {
                _shouldMediaControlBeVisible.postValue(
                    it && _activeMediaSession.value != null && isMediaControlEnabled.value == true
                )
            }
            addSource(activeMediaSession) {
                _shouldMediaControlBeVisible.postValue(
                    it != null && isNotificationListenerEnabled.value == true && isMediaControlEnabled.value == true
                )
            }
        }

        with(viewModelScope) {
            launch {
                widgetPreferenceManager.addedWidgets.collectLatest {
                    _addedWidgets.postValue(it)
                }
            }

            launch {
                dateTimePreferenceManager.shouldUse24HrClock.collectLatest {
                    _shouldUse24HrClock.postValue(it)
                }
            }

            launch {
                dateTimePreferenceManager.dateTimeViewSize.collectLatest {
                    _clockSize.postValue(it)
                }
            }

            launch {
                mediaControlPreferenceManager.isMediaControlEnabled.collectLatest {
                    _isMediaControlEnabled.postValue(it)
                }
            }

            launch {
                val shouldShowWeather = dateTimePreferenceManager.shouldShowWeather.first()
                if (shouldShowWeather) {
                    initializeWeather()
                    attachWeatherSettingsListeners()
                } else {
                    _weatherInfo.postValue(null)
                }
            }

            launch {
                searchPreferenceManager.searchModuleOrder.collectLatest {
                    _searchResultOrder.postValue(it)
                }
            }

            launch {
                appearancePreferenceManager.isAppDrawerEnabled.collectLatest {
                    _isAllAppsScreenEnabled.postValue(it)
                }
            }

            launch {
                dateTimePreferenceManager.weatherApiKey.collectLatest {
                    weatherApiKey = it
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

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        _activeMediaSession.postValue(
            if (controllers?.isEmpty() == true) null
            else controllers?.first()
        )
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

    fun updateWidgetList(newList: List<AddedWidget>) {
        viewModelScope.launch {
            widgetPreferenceManager.updateWidgetList(newList)
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
            }

            is AddedWidget.StarlightWidget -> viewModelScope.launch {
                widgetPreferenceManager.removeStarlightWidget(widget.extensionName)
            }
        }
    }

    fun recheckNotificationListener() {
        isNotificationListenerEnabled.postValue(
            isNotificationListenerEnabled()
        )
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
                        weatherScope.coroutineContext.cancelChildren()
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
        if (isLoadingWeather) return

        isLoadingWeather = true

        openWeatherApi
            .run {
                latLong = location
                unit = weatherUnit ?: dateTimePreferenceManager.weatherUnit.first()
                apiKey = weatherApiKey
                getCurrentWeather()
            }
            .getOrNull()
            ?.let {
                _weatherInfo.postValue(
                    Pair(openWeatherApi.unit, it)
                )
            }
            ?: _weatherInfo.postValue(null)

        isLoadingWeather = false
    }

    private fun observeActiveMediaSession() {
        mediaSessionManager.addOnActiveSessionsChangedListener(
            this,
            notificationListenerStubComponent
        )

        val activeMediaSessions = mediaSessionManager.getActiveSessions(
            notificationListenerStubComponent
        )

        if (activeMediaSessions.isNotEmpty()) {
            _activeMediaSession.value = activeMediaSessions.first()
        } else {
            _activeMediaSession.value = null
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val notificationListenerStr =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")

        return notificationListenerStr != null && notificationListenerStr.contains(
            notificationListenerStubComponent.flattenToString()
        )
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
