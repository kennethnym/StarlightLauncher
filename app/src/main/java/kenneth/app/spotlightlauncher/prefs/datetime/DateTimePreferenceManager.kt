package kenneth.app.spotlightlauncher.prefs.datetime

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.LatLong
import kenneth.app.spotlightlauncher.api.TemperatureUnit
import javax.inject.Inject
import javax.inject.Singleton

private object DefaultValue {
    const val SHOULD_SHOW_WEATHER = true
    const val SHOULD_USE_24HR_CLOCK = false
    val WEATHER_UNIT = TemperatureUnit.METRIC.name
}

@Singleton
class DateTimePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val shouldUse24HrClock: Boolean
        get() = sharedPreference.getBoolean(use24HrClockPrefKey, DefaultValue.SHOULD_USE_24HR_CLOCK)

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
        get() = sharedPreference.getBoolean(showWeatherPrefKey, DefaultValue.SHOULD_SHOW_WEATHER)

    val weatherUnit: TemperatureUnit
        get() = TemperatureUnit.valueOf(
            sharedPreference.getString(
                weatherUnitPrefKey,
                DefaultValue.WEATHER_UNIT
            ) ?: DefaultValue.WEATHER_UNIT
        )

    private val sharedPreference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private val use24HrClockPrefKey by lazy { context.getString(R.string.date_time_use_24hr_clock) }
    private val showWeatherPrefKey by lazy { context.getString(R.string.date_time_show_weather) }
    private val weatherUnitPrefKey by lazy { context.getString(R.string.date_time_weather_unit) }
    private val weatherLocationLatPrefKey by lazy { context.getString(R.string.date_time_weather_location_lat) }
    private val weatherLocationLongPrefKey by lazy { context.getString(R.string.date_time_weather_location_long) }
    private val weatherLocationNamePrefKey by lazy { context.getString(R.string.date_time_weather_location_name) }

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