package kenneth.app.spotlightlauncher.searching

import com.github.keelar.exprk.Expressions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kenneth.app.spotlightlauncher.api.DuckDuckGoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

enum class SuggestedResultType {
    NONE, MATH, WIFI, BLUETOOTH
}

typealias WebResultCallback = (webResult: SmartSearcher.WebResult) -> Unit

@Module
@InstallIn(ActivityComponent::class)
object SmartSearcherModule {
    @Provides
    fun provideExpressionParser() = Expressions()

    @Provides
    fun provideDuckDuckGoApiClient(httpClient: OkHttpClient) = DuckDuckGoApi(httpClient)
}

/**
 * SmartSearcher produces suggested results to the user.
 */
class SmartSearcher @Inject constructor(
    private val expressions: Expressions,
    private val duckduckgoApi: DuckDuckGoApi
) {
    private var activeApiCall: DuckDuckGoApi.Call? = null
    private var webResultListener: WebResultCallback? = null

    fun search(keyword: String): SuggestedResult {
        return try {
            // first, try to parse the query as a math expression.

            val result = parseAsMathExpression(keyword)

            SuggestedResult(
                query = keyword,
                type = SuggestedResultType.MATH,
                result = result,
            )
        } catch (e: Exception) {
            // the query is not a valid math expression

            when {
                // wifi command
                keyword.contains("wifi", ignoreCase = true) ->
                    SuggestedResult(
                        query = keyword,
                        type = SuggestedResultType.WIFI
                    )
                // bluetooth command
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

    /**
     * Cancels any ongoing call to duckduckgo api
     */
    fun cancelWebSearch() {
        activeApiCall?.cancel()
    }

    /**
     * Searches DuckDuckGo for definitions of the query.
     */
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