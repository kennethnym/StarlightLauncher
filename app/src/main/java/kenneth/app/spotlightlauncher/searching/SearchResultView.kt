package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.setPadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.SearchResultLayoutBinding
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var searchSearchResultAdapter: SearchResultAdapter

    private val binding: SearchResultLayoutBinding

    init {
        fitsSystemWindows = false
        orientation = VERTICAL
        gravity = Gravity.START

        setPadding(resources.getDimensionPixelSize(R.dimen.widget_margin_horizontal))

        binding = SearchResultLayoutBinding.inflate(LayoutInflater.from(context), this).also {
            BindingRegister.searchResultViewBinding = it
        }

        with(searcher) {
            addSearchResultListener { result, type ->
                activity?.runOnUiThread {
                    searchSearchResultAdapter.displayResult(result, type)
                }
            }

            setWebResultListener { result ->
                activity?.runOnUiThread {
                    searchSearchResultAdapter.displayWebResult(result)
                }
            }
        }
    }
}