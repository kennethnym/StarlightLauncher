package kenneth.app.starlightlauncher.prefs.datetime

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.TemperatureUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages preference for date & time for this launcher.
 */
@Singleton
internal class DateTimePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
) {
    val keys = DateTimePrefKeys(context)

    val shouldUse24HrClock
        get() = sharedPreferences.getBoolean(
            keys.use24HrClock,
            context.resources.getBoolean(R.bool.default_use_24hr_clock)
        )

    val dateTimeViewSize
        get() = DateTimeViewSize.valueOf(
            sharedPreferences.getString(
                keys.clockSize,
                null,
            ) ?: context.getString(R.string.default_clock_size)
        )

    /**
     * The pair of latitude and longitude that describes the location where the user wants
     * the weather info of.
     */
    val weatherLocation: LatLong
        get() {
            val lat = sharedPreferences.getFloat(keys.weatherLocationLat, 0f)
            val long = sharedPreferences.getFloat(keys.weatherLocationLong, 0f)
            return LatLong(lat, long)
        }

    val weatherLocationName
        get() = sharedPreferences.getString(
            keys.weatherLocationName, context.getString(
                R.string.date_time_unknown_location
            )
        ) ?: ""

    val shouldShowWeather
        get() = sharedPreferences.getBoolean(
            keys.showWeather,
            context.resources.getBoolean(R.bool.default_show_weather)
        )

    val weatherUnit
        get() = TemperatureUnit.valueOf(
            sharedPreferences.getString(keys.weatherUnit, null)
                ?: context.getString(R.string.default_weather_unit)
        )

    val shouldUseAutoWeatherLocation
        get() = sharedPreferences.getBoolean(
            keys.useAutoWeatherLocation,
            context.resources.getBoolean(R.bool.default_use_auto_weather_location)
        )

    val autoWeatherLocationCheckFrequency
        get() = sharedPreferences.getString(keys.autoWeatherLocationCheckFrequency, null)
            ?.toLong()
            ?: context.getString(R.string.default_check_weather_frequency).toLong()

    /**
     * Changes the weather location described by the given LatLong.
     */
    fun changeWeatherLocation(latLong: LatLong, displayName: String) {
        sharedPreferences.edit(commit = true) {
            putFloat(keys.weatherLocationLat, latLong.lat)
            putFloat(keys.weatherLocationLong, latLong.long)
            putString(keys.weatherLocationName, displayName)
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