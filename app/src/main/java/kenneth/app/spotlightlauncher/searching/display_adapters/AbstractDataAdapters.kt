package kenneth.app.spotlightlauncher.searching.display_adapters

import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.MainActivity

/**
 * A regular adapter interface to display search results. If the result needs to be displayed in
 * a RecyclerView, use SectionRecyclerViewAdapter.
 */
abstract class SectionResultAdapter<T>(protected val activity: MainActivity) {
    abstract fun displayResult(result: T)
}

interface SearchResultAdapter<T> {
    fun displayResult(result: T)
    fun hideResult()
}
