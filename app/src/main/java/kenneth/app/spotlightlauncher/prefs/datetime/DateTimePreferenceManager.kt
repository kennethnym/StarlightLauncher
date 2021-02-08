package kenneth.app.spotlightlauncher.prefs.datetime

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.LatLong
import kenneth.app.spotlightlauncher.api.TemperatureUnit

private object DefaultValue {
    const val SHOULD_SHOW_WEATHER = true
    val WEATHER_UNIT = TemperatureUnit.METRIC.name
}

object DateTimePreferenceManager {
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

    private lateinit var context: Context
    private val sharedPreference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private val showWeatherPrefKey by lazy { context.getString(R.string.date_time_show_weather) }
    private val weatherUnitPrefKey by lazy { context.getString(R.string.date_time_weather_unit) }
    private val weatherLocationLatPrefKey by lazy { context.getString(R.string.date_time_weather_location_lat) }
    private val weatherLocationLongPrefKey by lazy { context.getString(R.string.date_time_weather_location_long) }
    private val weatherLocationNamePrefKey by lazy { context.getString(R.string.date_time_weather_location_name) }

    fun getInstance(context: Context) = this.apply {
        this.context = context
    }

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