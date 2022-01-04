package kenneth.app.spotlightlauncher.searching

import android.util.Patterns
import com.github.keelar.exprk.Expressions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import kenneth.app.spotlightlauncher.api.DuckDuckGoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SuggestedResultType {
    NONE, MATH, WIFI, BLUETOOTH, URL, APP
}

typealias WebResultCallback = (webResult: SmartSearcher.WebResult) -> Unit

@Module
@InstallIn(SingletonComponent::class)
object SmartSearcherModule {
    @Provides
    fun provideExpressionParser() = Expressions()
}

/**
 * SmartSearcher produces suggested results to the user.
 */
@Singleton
class SmartSearcher @Inject constructor(
    private val expressions: Expressions,
    private val duckduckgoApi: DuckDuckGoApi
) {
    private var activeApiCall: DuckDuckGoApi.Call? = null
    private var webResultListener: WebResultCallback? = null

//    fun search(keyword: String): SearchResult.Suggested {
//        return try {
//            // first, try to parse the query as a math expression.
//
//            val result = parseAsMathExpression(keyword)
//
//            SearchResult.Suggested.Math(
//                query = keyword,
//                result,
//            )
//        } catch (e: Exception) {
//            // the query is not a valid math expression
//
//            when {
//                // wifi command
//                keyword.contains("wifi", ignoreCase = true) ->
//                    SearchResult.Suggested.Wifi(keyword)
//                // bluetooth command
//                keyword.contains("bluetooth", ignoreCase = true) ->
//                    SearchResult.Suggested.Bluetooth(keyword)
//                Patterns.WEB_URL.matcher(keyword).matches() ->
//                    SearchResult.Suggested.Url(keyword)
//                else -> SearchResult.Suggested.None(keyword)
//            }
//        }
//    }

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
                                        previewUrl = it.icon.fullUrl,
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