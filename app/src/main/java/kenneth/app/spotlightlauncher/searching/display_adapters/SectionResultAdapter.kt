package kenneth.app.spotlightlauncher.searching.display_adapters

import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.MainActivity

/**
 * An interface that adapters of each search result section have to implement
 */
abstract class SectionRecyclerViewAdapter<T, VH>() :
    RecyclerView.Adapter<VH>() where VH : RecyclerView.ViewHolder {
    protected lateinit var activity: MainActivity

    abstract fun getInstance(activity: MainActivity): SectionRecyclerViewAdapter<T, VH>

    /**
     * Display the given search result to the user
     */
    abstract fun displayResult(result: T)
}

abstract class SectionResultAdapter<T>(protected val activity: MainActivity) {
    abstract fun displayResult(result: T)
}
