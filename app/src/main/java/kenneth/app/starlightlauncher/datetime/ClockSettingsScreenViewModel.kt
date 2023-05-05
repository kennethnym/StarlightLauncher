package kenneth.app.starlightlauncher.datetime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.Place
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ClockSettingsScreenViewModel @Inject constructor(
    private val dateTimePreferenceManager: DateTimePreferenceManager,
) : ViewModel() {
    var dateTimeViewSize by mutableStateOf(DEFAULT_DATE_TIME_VIEW_SIZE)
        private set

    var shouldUse24HrClock by mutableStateOf(DEFAULT_USE_24HR_CLOCK)
        private set

    var shouldShowWeather by mutableStateOf(DEFAULT_SHOW_WEATHER)
        private set

    var shouldUseAutoWeatherLocation by mutableStateOf(DEFAULT_USE_AUTO_LOCATION)
        private set

    var weatherCheckFrequency by mutableStateOf(DEFAULT_WEATHER_CHECK_FREQUENCY)
        private set

    var locationCheckFrequency by mutableStateOf(DEFAULT_LOCATION_CHECK_FREQUENCY)
        private set

    var weatherUnit by mutableStateOf(DEFAULT_WEATHER_UNIT)
        private set

    var weatherLocationName by mutableStateOf<String?>(null)
        private set

    var weatherApiKey by mutableStateOf(BuildConfig.OPEN_WEATHER_API_KEY)
        private set

    init {
        with(viewModelScope) {
            launch {
                dateTimePreferenceManager.dateTimeViewSize.collectLatest {
                    dateTimeViewSize = it
                }
            }
            launch {
                dateTimePreferenceManager.shouldUse24HrClock.collectLatest {
                    shouldUse24HrClock = it
                }
            }
            launch {
                dateTimePreferenceManager.shouldShowWeather.collectLatest {
                    shouldShowWeather = it
                }
            }
            launch {
                dateTimePreferenceManager.shouldUseAutoWeatherLocation.collectLatest {
                    shouldUseAutoWeatherLocation = it
                }
            }
            launch {
                dateTimePreferenceManager.weatherCheckFrequency.collectLatest {
                    weatherCheckFrequency = it
                }
            }
            launch {
                dateTimePreferenceManager.locationCheckFrequency.collectLatest {
                    locationCheckFrequency = it
                }
            }
            launch {
                dateTimePreferenceManager.weatherUnit.collectLatest {
                    weatherUnit = it
                }
            }
            launch {
                dateTimePreferenceManager.weatherLocationName.collectLatest {
                    weatherLocationName = it
                }
            }
            launch {
                dateTimePreferenceManager.weatherApiKey.collectLatest {
                    weatherApiKey = it
                }
            }
        }
    }

    fun changeShouldShowWeather(shouldShow: Boolean) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeShouldShowWeather(shouldShow)
        }
    }

    fun changeShouldUse24HrClock(shouldUse: Boolean) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeShouldUse24HrClock(shouldUse)
        }
    }

    fun changeShouldUseAutoWeatherLocation(shouldUse: Boolean) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeShouldUseAutoWeatherLocation(shouldUse)
        }
    }

    fun changeDateTimeViewSize(size: DateTimeViewSize) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeDateTimeViewSize(size)
        }
    }

    fun changeLocationCheckFrequency(frequency: Long) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeLocationCheckFrequency(frequency)
        }
    }

    fun changeWeatherCheckFrequency(frequency: Long) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeWeatherCheckFrequency(frequency)
        }
    }

    fun changeWeatherUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeWeatherUnit(unit)
        }
    }

    fun changeWeatherLocation(place: Place) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeWeatherLocation(
                LatLong(place.lat, place.long),
                place.displayName
            )
        }
    }

    fun changeWeatherApiKey(apiKey: String) {
        viewModelScope.launch {
            dateTimePreferenceManager.changeWeatherApiKey(apiKey)
        }
    }

    fun useDefaultWeatherApiKey() {
        viewModelScope.launch {
            dateTimePreferenceManager.useDefaultWeatherApiKey()
        }
    }
}