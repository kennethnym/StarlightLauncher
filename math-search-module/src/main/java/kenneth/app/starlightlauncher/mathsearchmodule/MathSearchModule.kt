package kenneth.app.starlightlauncher.mathsearchmodule

import android.content.Context
import com.github.keelar.exprk.Expressions
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import java.math.BigDecimal

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.mathsearchmodule"

class MathSearchModule(context: Context) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = EXTENSION_NAME,
        displayName = context.getString(R.string.math_search_module_display_name),
        description = context.getString(R.string.math_search_module_description),
    )

    override val adapter: SearchResultAdapter
        get() = mathSearchResultAdapter

    private lateinit var mathSearchResultAdapter: MathSearchResultAdapter

    override fun initialize(launcher: StarlightLauncherApi) {
        mathSearchResultAdapter = MathSearchResultAdapter(launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        try {
            Result(
                query = keyword,
                value = Expressions().eval(keyword),
            )
        } catch (ex: Exception) {
            SearchResult.None(keyword, EXTENSION_NAME)
        }

    class Result(query: String, val value: BigDecimal) : SearchResult(query, EXTENSION_NAME)
}