package kenneth.app.spotlightlauncher.searching

import com.github.keelar.exprk.Expressions

enum class SuggestedResultType {
    NONE, MATH
}

class SmartSearcher {
    private val expressions = Expressions()

    fun search(keyword: String): SuggestedResult {
        return try {
            val result = parseAsMathExpression(keyword)

            SuggestedResult(
                query = keyword,
                type = SuggestedResultType.MATH,
                result = result,
            )
        } catch (e: Exception) {
            SuggestedResult(
                query = keyword,
                type = SuggestedResultType.NONE
            )
        }
    }

    private fun parseAsMathExpression(expression: String): Float? =
        expressions.eval(expression).toFloat()

    data class SuggestedResult(
        val query: String,
        val type: SuggestedResultType,
        val result: Any? = null
    )
}