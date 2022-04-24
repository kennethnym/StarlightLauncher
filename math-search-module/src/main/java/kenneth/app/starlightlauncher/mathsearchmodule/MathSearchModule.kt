package kenneth.app.starlightlauncher.mathsearchmodule

import com.github.keelar.exprk.ExpressionException
import com.github.keelar.exprk.Expressions
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import java.math.BigDecimal

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.mathsearchmodule"

class MathSearchModule : SearchModule {
    override lateinit var metadata: SearchModule.Metadata
        private set

    override val adapter: SearchResultAdapter
        get() = mathSearchResultAdapter

    private lateinit var mathSearchResultAdapter: MathSearchResultAdapter

    override fun initialize(launcher: StarlightLauncherApi) {
        val mainContext = launcher.context

        metadata = SearchModule.Metadata(
            extensionName = EXTENSION_NAME,
            displayName = mainContext.getString(R.string.math_search_module_display_name),
            description = mainContext.getString(R.string.math_search_module_description),
        )

        mathSearchResultAdapter = MathSearchResultAdapter(launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        try {
            Result(
                query = keyword,
                value = Expressions().eval(keyword),
            )
        } catch (ex: ExpressionException) {
            SearchResult.None(keyword, EXTENSION_NAME)
        }

    class Result(query: String, val value: BigDecimal) : SearchResult(query, EXTENSION_NAME)
}