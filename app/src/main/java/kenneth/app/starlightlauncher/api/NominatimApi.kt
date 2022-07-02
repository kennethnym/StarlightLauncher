package kenneth.app.starlightlauncher.api

import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

private const val API_URL = "https://nominatim.openstreetmap.org"

/**
 * An API wrapper around the Nominatim API, a Geocoder API powered by OpenStreetMap.
 */
class NominatimApi @Inject constructor(
    private val json: Json,
    private val httpClient: OkHttpClient,
    @Named(MAIN_DISPATCHER) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_DISPATCHER) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun searchForLocations(query: String) = withContext(mainDispatcher) {
        val url = "$API_URL/search".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build()
        val req = Request.Builder().url(url).build()

        return@withContext runCatching {
            withContext(ioDispatcher) {
                httpClient.newCall(req).execute().body?.string()
                    ?.let { json.decodeFromString<List<Place>>(it) }
            }
        }
    }

    suspend fun reverseGeocode(latLong: LatLong) = withContext(mainDispatcher) {
        val (lat, long) = latLong
        val url = "$API_URL/reverse".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("lat", lat.toString())
            .addQueryParameter("lon", long.toString())
            .addQueryParameter("format", "json")
            .build()

        val req = Request.Builder().url(url).build()

        return@withContext runCatching {
            withContext(ioDispatcher) {
                httpClient.newCall(req).execute().body?.string()
                    ?.let { json.decodeFromString<Place>(it) }
            }
        }
    }
}

/**
 * Describes a place returned by Nominatim's api.
 */
@Serializable
data class Place(
    @SerialName("display_name")
    val displayName: String,

    val lat: Float,

    @SerialName("lon")
    val long: Float,
)
