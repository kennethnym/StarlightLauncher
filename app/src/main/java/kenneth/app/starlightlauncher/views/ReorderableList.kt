package kenneth.app.starlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

typealias ListOrderChangedListener = (fromIndex: Int, toIndex: Int) -> Unit

class ReorderableList(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {
    private val orderObservable = object : Observable() {
        fun notifyOrderChanged(from: Int, to: Int) {
            setChanged()
            notifyObservers(intArrayOf(from, to))
        }

        fun addOrderObserver(observer: ListOrderChangedListener) {
            addObserver { _, arg ->
                if (arg is IntArray) {
                    observer(arg[0], arg[1])
                }
            }
        }
    }

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
                recyclerView.adapter?.notifyItemMoved(from, to)
                orderObservable.notifyOrderChanged(from, to)
                return true
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}
        })

    init {
        dndTouchHelper.attachToRecyclerView(this)
    }

    fun addOnOrderChangedListener(listener: ListOrderChangedListener) {
        orderObservable.addOrderObserver(listener)
    }
}
