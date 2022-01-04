package kenneth.app.spotlightlauncher.prefs.searchlayout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.databinding.SearchCategoryOrderListItemBinding
import kenneth.app.spotlightlauncher.prefs.SearchPreferenceManager
import kenneth.app.spotlightlauncher.searching.SearchModuleManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import javax.inject.Inject

@AndroidEntryPoint
class SearchCategoryOrderList(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {
    @Inject
    lateinit var searchModuleManager: SearchModuleManager

    @Inject
    lateinit var searchPreferenceManager: SearchPreferenceManager

    private lateinit var categoryOrder: MutableList<String>

    private val dndTouchHelper =
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView
                        ?.animate()
                        ?.scaleX(1.1f)
                        ?.scaleY(1.1f)
                        ?.alpha(0.5f)
                        ?.setDuration(200)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView
                    .animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                categoryOrder[from] = categoryOrder[to].also {
                    categoryOrder[to] = categoryOrder[from]
                }
                recyclerView.adapter?.notifyItemMoved(from, to)
                searchPreferenceManager.changeSearchCategoryOrder(categoryOrder)
                return true
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}
        })

    fun showItems() {
        val adapter = SearchCategoryOrderListAdapter(context, searchModuleManager).apply {
            data = searchPreferenceManager.categoryOrder
        }

        categoryOrder = searchPreferenceManager.categoryOrder.toMutableList()
        this.adapter = adapter
        layoutManager = adapter.layoutManager
        dndTouchHelper.attachToRecyclerView(this)
    }
}

private class SearchCategoryOrderListAdapter(
    private val context: Context,
    private val searchModuleManager: SearchModuleManager,
) : RecyclerViewDataAdapter<String, SearchCategoryOrderListItem>() {
    override var data = listOf<String>()

    override val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchCategoryOrderListItem {
        val binding =
            SearchCategoryOrderListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return SearchCategoryOrderListItem(binding, searchModuleManager)
    }
}

private class SearchCategoryOrderListItem(
    override val binding: SearchCategoryOrderListItemBinding,
    private val searchModuleManager: SearchModuleManager,
) :
    RecyclerViewDataAdapter.ViewHolder<String>(binding) {
    override fun bindWith(data: String) {
        searchModuleManager.lookupSearchModule(data)?.let {
            binding.apply {
                categoryTitle = it.displayName
                categoryDescription = it.description
            }
        }
    }
}
