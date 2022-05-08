package kenneth.app.starlightlauncher.prefs.datetime

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.TemperatureUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DateTimePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val shouldUse24HrClock: Boolean
        get() = sharedPreference.getBoolean(
            use24HrClockPrefKey,
            context.resources.getBoolean(R.bool.default_use_24hr_clock)
        )

    /**
     * The pair of latitude and longitude that describes the location where the user wants
     * the weather info of.
     */
    val weatherLocation: LatLong
        get() {
            val lat = sharedPreference.getFloat(weatherLocationLatPrefKey, 0f)
            val long = sharedPreference.getFloat(weatherLocationLongPrefKey, 0f)
            return LatLong(lat, long)
        }

    val weatherLocationName: String
        get() = sharedPreference.getString(
            weatherLocationNamePrefKey, context.getString(
                R.string.date_time_unknown_location
            )
        ) ?: ""

    val shouldShowWeather: Boolean
        get() = sharedPreference.getBoolean(
            showWeatherPrefKey,
            context.resources.getBoolean(R.bool.default_show_weather)
        )

    val weatherUnit: TemperatureUnit
        get() = TemperatureUnit.valueOf(
            sharedPreference.getString(weatherUnitPrefKey, null)
                ?: context.getString(R.string.default_weather_unit)
        )

    val shouldUseAutoWeatherLocation: Boolean
        get() = sharedPreference.getBoolean(
            useAutoWeatherLocationKey,
            context.resources.getBoolean(R.bool.default_use_auto_weather_location)
        )

    val autoWeatherLocationCheckFrequency: Long
        get() = sharedPreference.getString(autoWeatherLocationCheckFrequencyKey, null)
            ?.toLong()
            ?: context.getString(R.string.default_check_weather_frequency).toLong()

    private val sharedPreference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    val use24HrClockPrefKey by lazy { context.getString(R.string.date_time_use_24hr_clock) }
    val showWeatherPrefKey by lazy { context.getString(R.string.date_time_show_weather) }
    val weatherUnitPrefKey by lazy { context.getString(R.string.date_time_weather_unit) }
    val weatherLocationLatPrefKey by lazy { context.getString(R.string.date_time_weather_location_lat) }
    val weatherLocationLongPrefKey by lazy { context.getString(R.string.date_time_weather_location_long) }
    val weatherLocationNamePrefKey by lazy { context.getString(R.string.date_time_weather_location_name) }
    val useAutoWeatherLocationKey by lazy { context.getString(R.string.date_time_use_auto_location) }
    val autoWeatherLocationCheckFrequencyKey by lazy { context.getString(R.string.date_time_auto_location_check_frequency) }

    /**
     * Changes the weather location described by the given LatLong.
     */
    fun changeWeatherLocation(latLong: LatLong, displayName: String) {
        sharedPreference.edit()
            .putFloat(weatherLocationLatPrefKey, latLong.lat)
            .putFloat(weatherLocationLongPrefKey, latLong.long)
            .putString(weatherLocationNamePrefKey, displayName)
            .apply()
    }
}