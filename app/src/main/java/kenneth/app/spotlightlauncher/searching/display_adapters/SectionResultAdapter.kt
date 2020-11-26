package kenneth.app.spotlightlauncher.searching.display_adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.MainActivity

/**
 * A singleton abstraction over RecyclerView.Adapter that delegates data binding (onBindViewHolder)
 * to RecyclerViewDataAdapter.ViewHolder. Clients need to implement:
 *   - getInstance(activity), which binds this adapter to a particular RecyclerView in activity;
 *   - displayData(data), which clients can call to display data to the users.
 *
 * This abstraction encourages separation of view logic of individual items
 * from view logic of the RecyclerView itself. View manipulation relating to the RecyclerView
 * should be handled in displayData.
 */
abstract class RecyclerViewDataAdapter<T, VH : RecyclerViewDataAdapter.ViewHolder<T>> :
    RecyclerView.Adapter<VH>() {
    /**
     * The data to be displayed by this adapter. Exposed to allow access to the data this adapter
     * is holding.
     */
    lateinit var data: List<T>

    /**
     * The MainActivity that the RecyclerView is in.
     */
    protected lateinit var activity: MainActivity

    /**
     * Gets an instance of this adapter, and also binds this adapter to a particular
     * RecyclerView.
     */
    abstract fun getInstance(activity: MainActivity): RecyclerViewDataAdapter<T, VH>

    /**
     * Displays the given data in the RecyclerView this adapter is bound to.
     */
    abstract fun displayData(data: List<T>?)

    /**
     * This is responsible for holding views for this adapter. View logic of individual
     * items in the RecyclerView should be handled in this class.
     */
    abstract class ViewHolder<T>(
        protected val view: View,
        protected val activity: MainActivity
    ) : RecyclerView.ViewHolder(view) {
        /**
         * Binds this ViewHolder with the given data, and also displays the data.
         */
        abstract fun bindWith(data: T)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.bindWith(item)
    }

    override fun getItemCount() = data.size
}

/**
 * A regular adapter interface to display search results. If the result needs to be displayed in
 * a RecyclerView, use SectionRecyclerViewAdapter.
 */
abstract class SectionResultAdapter<T>(protected val activity: MainActivity) {
    abstract fun displayResult(result: T)
}
