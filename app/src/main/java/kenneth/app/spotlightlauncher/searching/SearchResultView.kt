package kenneth.app.spotlightlauncher.searching

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.setPadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.databinding.SearchResultLayoutBinding
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.utils.dp
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var searchResultAdapter: ResultAdapter

    private val binding: SearchResultLayoutBinding

    init {
        fitsSystemWindows = false
        orientation = VERTICAL
        gravity = Gravity.START

        setPadding(16.dp)

        binding = SearchResultLayoutBinding.inflate(LayoutInflater.from(context), this).also {
            BindingRegister.searchResultViewBinding = it
        }

        with(searcher) {
            setSearchResultListener { result, type ->
                activity?.runOnUiThread {
                    searchResultAdapter.displayResult(result, type)
                }
            }

            setWebResultListener { result ->
                activity?.runOnUiThread {
                    searchResultAdapter.displayWebResult(result)
                }
            }
        }
    }
}