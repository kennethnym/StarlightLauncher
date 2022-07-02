package kenneth.app.starlightlauncher.prefs.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.ReorderablePreferenceItemBinding
import kenneth.app.starlightlauncher.views.ListOrderChangedListener
import kenneth.app.starlightlauncher.views.ReorderableList

/**
 * A preference view that allows users to reorder items by drag-n-drop.
 */
internal class ReorderablePreference(context: Context, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {
    private var orderListener: ListOrderChangedListener? = null

    var items: List<Item> = emptyList()

    init {
        layoutResource = R.layout.reorderable_preference_layout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(R.id.order_list) as ReorderableList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ReorderablePreferenceListAdapter(items)
            addOnOrderChangedListener(::onOrderChanged)
        }
    }

    /**
     * Registers [listener] to be called when the order of items has changed.

     * Only one can be registered at a time.
     * Subsequent registrations replace the listener registered previously.
     */
    fun setOnPreferenceOrderChanged(listener: ListOrderChangedListener) {
        orderListener = listener
    }

    private fun onOrderChanged(fromIndex: Int, toIndex: Int) {
        orderListener?.let { it(fromIndex, toIndex) }
    }

    /**
     * Represents an item in [ReorderablePreference].
     * Use [ReorderablePreference.items] to set the list of items to show in [ReorderablePreference]
     */
    data class Item(
        /**
         * The value this item stores.
         */
        val value: String,
        /**
         * The title of this item. Equivalent to Preference title.
         */
        val title: String,
        /**
         * The summary of this item. Equivalent to Preference summary.
         */
        val summary: String,
    )
}

private class ReorderablePreferenceListAdapter(private val items: List<ReorderablePreference.Item>) :
    RecyclerView.Adapter<ReorderablePreferenceListItem>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReorderablePreferenceListItem {
        val binding = ReorderablePreferenceItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReorderablePreferenceListItem(binding)
    }

    override fun onBindViewHolder(holder: ReorderablePreferenceListItem, position: Int) {
        val item = items[position]
        with(holder.binding) {
            title = item.title
            summary = item.summary
        }
    }

    override fun getItemCount(): Int = items.size
}

private class ReorderablePreferenceListItem(
    val binding: ReorderablePreferenceItemBinding
) : RecyclerView.ViewHolder(binding.root)
