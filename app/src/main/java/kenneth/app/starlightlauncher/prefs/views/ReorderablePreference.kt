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

class ReorderablePreference(context: Context, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {
    private lateinit var orderList: ReorderableList

    private var orderListener: ListOrderChangedListener? = null

    var items: List<Item> = emptyList()

    init {
        layoutResource = R.layout.reorderable_preference_layout
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        (holder?.findViewById(R.id.order_list) as? ReorderableList)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ReorderablePreferenceListAdapter(items)
            addOnOrderChangedListener(::onOrderChanged)
        }
    }

    fun setOnPreferenceOrderChanged(listener: ListOrderChangedListener) {
        orderListener = listener
    }

    private fun onOrderChanged(fromIndex: Int, toIndex: Int) {
        orderListener?.let { it(fromIndex, toIndex) }
    }

    data class Item(
        val value: String,
        val title: String,
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
