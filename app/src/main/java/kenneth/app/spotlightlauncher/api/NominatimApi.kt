package kenneth.app.spotlightlauncher.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.Exception
import javax.inject.Inject

private const val API_URL = "https://nominatim.openstreetmap.org"

/**
 * An API wrapper around the Nominatim API, a Geocoder API powered by OpenStreetMap.
 */
class NominatimApi @Inject constructor(
    private val json: Json,
    private val httpClient: OkHttpClient
) {
    suspend fun searchForLocations(query: String): List<Place>? {
        val url = HttpUrl.parse("$API_URL/search")!!
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build()

        val req = Request.Builder().url(url).build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(req).execute()
            }
            val body = response.body()?.string() ?: return null

            Log.d("spotlight", body)

            json.decodeFromString<List<Place>>(body)
        } catch (ex: Exception) {
            throw ex
        }
    }
}

@Serializable
data class Place(
    @SerialName("display_name")
    val displayName: String,

    val lat: Float,

    @SerialName("lon")
    val long: Float,
)
