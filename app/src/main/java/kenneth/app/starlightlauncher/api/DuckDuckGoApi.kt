package kenneth.app.starlightlauncher.api

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

private const val API_URL_STRING = "https://api.duckduckgo.com/"
private val apiUrl = HttpUrl.parse(API_URL_STRING)!!

/**
 * Creates a URL that redirects to duckduckgo search page with the given query.
 */
fun getDuckDuckGoRedirectUrlFromQuery(query: String): Uri = Uri.parse(
    apiUrl.newBuilder()
        .addQueryParameter("q", query)
        .build()
        .toString()
)

class DuckDuckGoApi @Inject constructor(
    private val json: Json,
    private val httpClient: OkHttpClient,
) {
    fun newCall(query: String): Call {
        val url = apiUrl.newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        return Call(httpClient.newCall(request))
    }

    /**
     * Wrapper class around okhttp3.Call
     */
    inner class Call(private val httpCall: okhttp3.Call) {
        fun execute() = try {
            val response = httpCall.execute()
            val responseStr = response.body()?.string()

            if (responseStr != null) {
                json.decodeFromString<Result>(responseStr)
            } else {
                null
            }
        } catch (e: Exception) {
            throw e
        }

        fun cancel() = httpCall.cancel()

//        private fun parseJsonToResult(jsonStr: String): Result {
//            val jsonObject = Parser.default().parse(StringReader(jsonStr)) as JsonObject
//
//            return Result(
//                abstractText = jsonObject.string("AbstractText") ?: "",
//                heading = jsonObject.string("Heading") ?: "",
//                relatedTopics = jsonObject.array<JsonObject>("RelatedTopics")
//                    ?.flatMap {
//                        if (it.containsKey("Name") && it.containsKey("Topics")) {
//                            val arr = it.array<JsonObject>("Topics")
//
//                            if (arr != null) json.parseFromJsonArray(arr) ?: emptyList()
//                            else emptyList()
//                        } else {
//                            val parsed = json.parseFromJsonObject<Result.RelatedTopic>(it)
//
//                            if (parsed != null) listOf(parsed)
//                            else emptyList()
//                        }
//                    }
//                    ?.toList()
//                    ?: emptyList()
//            )
//        }
    }

    @Serializable
    data class Result(
        @SerialName("AbstractText")
        val abstractText: String,

        @SerialName("Heading")
        val heading: String,

        @SerialName("RelatedTopics")
        val relatedTopics: List<RelatedTopic>
    ) {
        @Serializable
        data class RelatedTopic(
            @SerialName("FirstURL")
            val firstUrl: String,

            @SerialName("Text")
            val text: String,

            @SerialName("Icon")
            val icon: Icon,
        )

        @Serializable
        data class Icon constructor(
            @SerialName("Width")
            val width: String,

            @SerialName("Height")
            val height: String,

            @SerialName("URL")
            val url: String,
        ) {
            @Transient
            val fullUrl = "$apiUrl$url"
        }
    }
}