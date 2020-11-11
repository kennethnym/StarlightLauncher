package kenneth.app.spotlightlauncher

import androidx.recyclerview.widget.RecyclerView

/**
 * An interface that adapters of each search result section have to implement
 */
abstract class SectionResultAdapter<T, VH>(protected val activity: MainActivity) :
    RecyclerView.Adapter<VH>() where VH : RecyclerView.ViewHolder {
    /**
     * Display the given search result to the user
     */
    abstract fun displayResult(result: T)
}
