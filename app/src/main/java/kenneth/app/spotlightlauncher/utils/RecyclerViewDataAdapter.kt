package kenneth.app.spotlightlauncher.utils

import android.app.Activity
import android.view.View
import androidx.annotation.CallSuper
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
     * The LayoutManager that RecyclerView should use.
     */
    abstract val layoutManager: RecyclerView.LayoutManager

    /**
     * The RecyclerView that this adapter should bind to.
     */
    protected abstract val recyclerView: RecyclerView

    /**
     * Determines if the adapter is bind to a recyclerview already.
     */
    private var isAdapterBind = false

    /**
     * The data to be displayed by this adapter. Exposed to allow access to the data this adapter
     * is holding.
     */
    lateinit var data: List<T>

    /**
     * The MainActivity that the RecyclerView is in.
     */
    protected lateinit var activity: Activity

    /**
     * Gets an instance of this adapter
     */
    abstract fun getInstance(activity: Activity): RecyclerViewDataAdapter<T, VH>

    /**
     * Displays the given data in the RecyclerView this adapter is bound to.
     * Automatically binds this adapter to
     */
    @CallSuper
    open fun displayData(data: List<T>?) {
        bindAdapterToRecyclerView()
    }

    private fun bindAdapterToRecyclerView() {
        if (!isAdapterBind) {
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = this
            isAdapterBind = true
        }
    }

    /**
     * Unbinds an adapter from the recycler view. Must be called before removing the recycler view.
     */
    fun unbindAdapterFromRecyclerView() {
        recyclerView.apply {
            layoutManager = null
            adapter = null
        }
        isAdapterBind = false
    }

    /**
     * This is responsible for holding views for this adapter. View logic of individual
     * items in the RecyclerView should be handled in this class.
     */
    abstract class ViewHolder<T>(
        protected val view: View,
        protected val activity: Activity
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
