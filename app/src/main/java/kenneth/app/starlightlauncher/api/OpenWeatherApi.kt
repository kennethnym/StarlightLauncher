package kenneth.app.starlightlauncher.api

import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

enum class TemperatureUnit(val code: String, val symbol: String) {
    KELVIN("standard", "K"),
    CELSIUS("metric", "°C"),
    IMPERIAL("imperial", "°F");
}

private const val API_URL = "https://api.openweathermap.org/data/2.5"
private const val WEATHER_ICON_URL = "https://openweathermap.org/img/wn"

/**
 * An api wrapper for OpenWeatherApi.
 */
class OpenWeatherApi @Inject constructor(
    private val json: Json,
    private val httpClient: OkHttpClient,
    @Named(MAIN_DISPATCHER) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Weather information returned by this API is based on the location this lat long pair describes.
     * Must be set before making API calls.
     */
    lateinit var latLong: LatLong

    /**
     * The unit that the temperature values returned by this API should be in.
     * Default is TemperatureUnit.METRIC. Can be STANDARD, METRIC or IMPERIAL.
     */
    var unit: TemperatureUnit = TemperatureUnit.CELSIUS

    /**
     * Fetches the current weather of the location specified by latLong
     */
    suspend fun getCurrentWeather() = withContext(mainDispatcher) {
        val (lat, long) = latLong
        val url = HttpUrl.parse("$API_URL/weather")!!
            .newBuilder()
            .addQueryParameter("units", unit.code)
            .addQueryParameter("lat", lat.toString())
            .addQueryParameter("lon", long.toString())
            .addQueryParameter("appid", BuildConfig.OPEN_WEATHER_API_KEY)
            .build()

        val req = Request.Builder().url(url).build()

        return@withContext runCatching {
            withContext(ioDispatcher) {
                httpClient.newCall(req).execute().body()?.string()
                    ?.let { json.decodeFromString<Response>(it) }
            }
        }
    }

    @Serializable
    data class Response(
        val main: MainWeatherInfo,
        val weather: List<Weather>,
    )
}

@Serializable
data class Weather(
    val id: Float,
    val main: String,
    val description: String,
    val icon: String,
) {
    @Transient
    val iconURL = "$WEATHER_ICON_URL/$icon.png"
}

@Serializable
data class MainWeatherInfo(
    val temp: Float,

    @SerialName("feels_like")
    val feelsLike: Float,

    @SerialName("temp_min")
    val minTemp: Float,

    @SerialName("temp_max")
    val maxTemp: Float,
)