package kenneth.app.spotlightlauncher.searching

import android.util.Log
import com.beust.klaxon.Json
import com.github.keelar.exprk.Expressions
import kenneth.app.spotlightlauncher.api.DuckDuckGoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

enum class SuggestedResultType {
    NONE, MATH, WIFI, BLUETOOTH
}

typealias WebResultCallback = (webResult: SmartSearcher.WebResult) -> Unit

class SmartSearcher {
    private val expressions = Expressions()
    private val http = OkHttpClient()
    private val duckduckgoApi = DuckDuckGoApi(http)

    private var activeApiCall: DuckDuckGoApi.Call? = null
    private var webResultListener: WebResultCallback? = null

    fun search(keyword: String): SuggestedResult {
        return try {
            val result = parseAsMathExpression(keyword)

            SuggestedResult(
                query = keyword,
                type = SuggestedResultType.MATH,
                result = result,
            )
        } catch (e: Exception) {
            when {
                keyword.contains("wifi", ignoreCase = true) ->
                    SuggestedResult(
                        query = keyword,
                        type = SuggestedResultType.WIFI
                    )
                keyword.contains("bluetooth", ignoreCase = true) ->
                    SuggestedResult(
                        query = keyword,
                        type = SuggestedResultType.BLUETOOTH,
                    )
                else -> SuggestedResult(
                    query = keyword,
                    type = SuggestedResultType.NONE,
                )
            }
        }
    }

    fun setWebResultListener(listener: WebResultCallback) {
        webResultListener = listener
    }

    fun cancelWebSearch() {
        activeApiCall?.cancel()
    }

    suspend fun performWebSearch(keyword: String) {
        try {
            activeApiCall = duckduckgoApi.newCall(keyword)
            val response = withContext(Dispatchers.IO) {
                activeApiCall!!.execute()
            }

            if (response != null) {
                webResultListener?.let {
                    it(
                        WebResult(
                            query = keyword,
                            title = response.heading,
                            content = response.abstractText,
                            relatedTopics = response.relatedTopics
                                .map {
                                    WebResult.Topic(
                                        title = it.text,
                                        url = it.firstUrl,
                                        previewUrl = it.icon.url,
                                    )
                                }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun parseAsMathExpression(expression: String) = expressions.eval(expression).toFloat()

    data class SuggestedResult(
        val query: String,
        val type: SuggestedResultType,
        val result: Any? = null,
    )

    data class WebResult(
        val query: String,
        val title: String,
        val content: String,
        val relatedTopics: List<Topic>
    ) {
        data class Topic(
            val url: String,
            val title: String,
            val previewUrl: String,
        )
    }
}