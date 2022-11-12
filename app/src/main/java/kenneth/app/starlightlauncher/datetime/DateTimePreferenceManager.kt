package kenneth.app.starlightlauncher.datetime

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.prefs.*
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages preference for date & time for this launcher.
 */
@Singleton
internal class DateTimePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val shouldUse24HrClock = context.dataStore.data.map {
        it[PREF_KEY_USE_24HR_CLOCK] ?: DEFAULT_USE_24HR_CLOCK
    }

    val dateTimeViewSize = context.dataStore.data.map { preferences ->
        preferences[PREF_KEY_CLOCK_SIZE]?.let {
            DateTimeViewSize.valueOf(it)
        } ?: DEFAULT_DATE_TIME_VIEW_SIZE
    }

    /**
     * The pair of latitude and longitude that describes the location where the user wants
     * the weather info of.
     */
    val weatherLocation = context.dataStore.data.map {
        val lat = it[PREF_KEY_WEATHER_LOCATION_LAT]
        val long = it[PREF_KEY_WEATHER_LOCATION_LONG]

        if (lat == null || long == null) null
        else LatLong(lat, long)
    }

    val weatherLocationName = context.dataStore.data.map {
        it[PREF_KEY_WEATHER_LOCATION_NAME]
    }

    val shouldShowWeather = context.dataStore.data.map {
        it[PREF_KEY_SHOW_WEATHER] ?: DEFAULT_SHOW_WEATHER
    }

    val weatherUnit = context.dataStore.data.map {
        it[PREF_KEY_WEATHER_UNIT]?.let { unit -> TemperatureUnit.valueOf(unit) }
            ?: DEFAULT_WEATHER_UNIT
    }

    val shouldUseAutoWeatherLocation = context.dataStore.data.map {
        it[PREF_KEY_USE_AUTO_LOCATION] ?: DEFAULT_USE_AUTO_LOCATION
    }

    val autoWeatherLocationCheckFrequency = context.dataStore.data.map {
        it[PREF_AUTO_LOCATION_CHECK_FREQUENCY]?.toLong() ?: DEFAULT_AUTO_LOCATION_CHECK_FREQUENCY
    }

    val weatherCheckFrequency = context.dataStore.data.map {
        it[PREF_KEY_WEATHER_CHECK_FREQUENCY] ?: DEFAULT_WEATHER_CHECK_FREQUENCY
    }

    val locationCheckFrequency = context.dataStore.data.map {
        it[PREF_KEY_LOCATION_CHECK_FREQUENCY] ?: DEFAULT_LOCATION_CHECK_FREQUENCY
    }

    /**
     * Changes the weather location described by the given LatLong.
     */
    suspend fun changeWeatherLocation(latLong: LatLong, displayName: String) {
        context.dataStore.edit {
            it[PREF_KEY_WEATHER_LOCATION_LAT] = latLong.lat
            it[PREF_KEY_WEATHER_LOCATION_LONG] = latLong.long
            it[PREF_KEY_WEATHER_LOCATION_NAME] = displayName
        }
    }

    suspend fun changeShouldShowWeather(shouldShow: Boolean) {
        context.dataStore.edit {
            it[PREF_KEY_SHOW_WEATHER] = shouldShow
        }
    }

    suspend fun changeDateTimeViewSize(size: DateTimeViewSize) {
        context.dataStore.edit {
            it[PREF_KEY_CLOCK_SIZE] = size.name
        }
    }

    suspend fun changeShouldUse24HrClock(shouldUse: Boolean) {
        context.dataStore.edit {
            it[PREF_KEY_USE_24HR_CLOCK] = shouldUse
        }
    }

    suspend fun changeShouldUseAutoWeatherLocation(shouldUse: Boolean) {
        context.dataStore.edit {
            it[PREF_KEY_USE_AUTO_LOCATION] = shouldUse
        }
    }

    suspend fun changeLocationCheckFrequency(frequency: Long) {
        context.dataStore.edit {
            it[PREF_KEY_LOCATION_CHECK_FREQUENCY] = frequency
        }
    }

    suspend fun changeWeatherCheckFrequency(frequency: Long) {
        context.dataStore.edit {
            it[PREF_KEY_WEATHER_CHECK_FREQUENCY] = frequency
        }
    }

    suspend fun changeWeatherUnit(unit: TemperatureUnit) {
        context.dataStore.edit {
            it[PREF_KEY_WEATHER_UNIT] = unit.code
        }
    }
}

internal class DateTimePrefKeys(context: Context) {
    val use24HrClock by lazy { context.getString(R.string.date_time_use_24hr_clock) }
    val clockSize by lazy { context.getString(R.string.date_time_clock_size) }
    val showWeather by lazy { context.getString(R.string.date_time_show_weather) }
    val weatherUnit by lazy { context.getString(R.string.date_time_weather_unit) }
    val weatherLocationLat by lazy { context.getString(R.string.date_time_weather_location_lat) }
    val weatherLocationLong by lazy { context.getString(R.string.date_time_weather_location_long) }
    val weatherLocationName by lazy { context.getString(R.string.date_time_weather_location_name) }
    val useAutoWeatherLocation by lazy { context.getString(R.string.date_time_use_auto_location) }
    val autoWeatherLocationCheckFrequency by lazy { context.getString(R.string.date_time_auto_location_check_frequency) }
}