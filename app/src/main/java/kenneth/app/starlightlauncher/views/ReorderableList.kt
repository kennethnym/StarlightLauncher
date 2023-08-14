package kenneth.app.starlightlauncher.views

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView] that allows reordering of items through drag-and-drop.
 */
internal open class ReorderableList(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {
    interface Listener {
        fun onItemDragStart(viewHolder: ViewHolder)

        fun onItemDragEnd(viewHolder: ViewHolder)

        fun onOrderChange(fromPosition: Int, toPosition: Int)
    }

    var listener: Listener? = null

    private val dndTouchHelper =
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView
                        .animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .alpha(0.5f).duration = 200
                    listener?.onItemDragStart(viewHolder)
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
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            listener?.onItemDragEnd(viewHolder)
                        }

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                listener?.onOrderChange(from, to)
                recyclerView.adapter?.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}
        })

    init {
        enableDragAndDrop()
    }

    /**
     * Disables drag and drop of items.
     */
    fun disableDragAndDrop() {
        dndTouchHelper.attachToRecyclerView(null)
    }

    /**
     * Enables drag and drop of items.
     */
    fun enableDragAndDrop() {
        dndTouchHelper.attachToRecyclerView(this)
    }
}
