package kenneth.app.spotlightlauncher.api

import android.net.Uri
import com.beust.klaxon.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
import javax.inject.Inject

val apiUrlString = "https://api.duckduckgo.com/"
val apiUrl = HttpUrl.parse(apiUrlString)!!

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
        private val json = Klaxon()
            .fieldConverter(Result.Icon.Url::class, iconUrlConverter)

        fun execute() = try {
            val response = httpCall.execute()
            val responseStr = response.body()?.string() ?: ""

            if (responseStr.isNotBlank()) {
                parseJsonToResult(responseStr)
            } else {
                null
            }
        } catch (e: Exception) {
            throw e
        }

        fun cancel() = httpCall.cancel()

        private fun parseJsonToResult(jsonStr: String): Result {
            val jsonObject = Parser.default().parse(StringReader(jsonStr)) as JsonObject

            return Result(
                abstractText = jsonObject.string("AbstractText") ?: "",
                heading = jsonObject.string("Heading") ?: "",
                relatedTopics = jsonObject.array<JsonObject>("RelatedTopics")
                    ?.flatMap {
                        if (it.containsKey("Name") && it.containsKey("Topics")) {
                            val arr = it.array<JsonObject>("Topics")

                            if (arr != null) json.parseFromJsonArray(arr) ?: emptyList()
                            else emptyList()
                        } else {
                            val parsed = json.parseFromJsonObject<Result.RelatedTopic>(it)

                            if (parsed != null) listOf(parsed)
                            else emptyList()
                        }
                    }
                    ?.toList()
                    ?: emptyList()
            )
        }
    }

    private val iconUrlConverter = object : Converter {
        override fun canConvert(cls: Class<*>) = cls == String::class.java

        override fun fromJson(jv: JsonValue) =
            if (jv.string == null || jv.string == "") jv.string
            else "$apiUrl${jv.string}"

        override fun toJson(value: Any): String = """{ "Icon": $value }"""
    }

    data class Result(
        @Json(name = "AbstractText")
        val abstractText: String,

        @Json(name = "Heading")
        val heading: String,

        @Json(name = "RelatedTopics")
        val relatedTopics: List<RelatedTopic>
    ) {
        data class RelatedTopic(
            @Json(name = "FirstURL")
            val firstUrl: String,

            @Json(name = "Text")
            val text: String,

            @Json(name = "Icon")
            val icon: Icon,
        )

        data class Icon @JvmOverloads constructor(
            @Json(name = "Width")
            val width: String,

            @Json(name = "Height")
            val height: String,

            @Json(name = "URL")
            @Icon.Url
            val url: String,
        ) {
            @Target(AnnotationTarget.FIELD)
            annotation class Url
        }
    }
}