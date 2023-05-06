package kenneth.app.starlightlauncher.widgets

import android.animation.ObjectAnimator
import android.appwidget.AppWidgetHost
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.views.ReorderableList
import javax.inject.Inject
import kotlin.math.abs

private const val SCROLL_THRESHOLD = 10

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
internal class WidgetListView(context: Context, attrs: AttributeSet) :
    ReorderableList(context, attrs), WidgetListAdapter.WidgetListEventListener {
    interface OnChangedListener {
        fun onRequestRemoveWidget(removedWidget: AddedWidget)

        fun onWidgetResized(widget: AddedWidget, newHeight: Int)

        fun onWidgetSwapped(oldPosition: Int, newPosition: Int)
    }

    @Inject
    lateinit var bindingRegister: BindingRegister

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    private val showAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        duration = 200
    }

    private val hideAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
        duration = 200
    }

    private val widgetListAdapter: WidgetListAdapter

    private var initialY: Float? = null

    private var isClick = false

    /**
     * The widget view holder currently in edit mode.
     */
    private var widgetViewHolderInEditMode: WidgetListAdapterItem? = null

    var widgets: List<AddedWidget> = emptyList()
        set(value) {
            widgetListAdapter.widgets = value
            DiffUtil.calculateDiff(DiffCallback(field, value))
                .dispatchUpdatesTo(widgetListAdapter)
            field = value
        }

    var onWidgetListChangedListener: OnChangedListener? = null

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        isNestedScrollingEnabled = true
        clipToPadding = false
        updatePadding(top = context.resources.getDimensionPixelSize(R.dimen.widget_list_space_between))

        layoutManager = LinearLayoutManager(context)
        adapter =
            WidgetListAdapter(context, widgets, appWidgetHost, this).also { widgetListAdapter = it }

        addOnOrderChangedListener { fromPosition, toPosition ->
            onWidgetListChangedListener?.onWidgetSwapped(fromPosition, toPosition)
        }
        addOnSelectionChangedListener(::onWidgetLongPressed)
        setOnApplyWindowInsetsListener { _, insets ->
            updatePadding(
                bottom =
                WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        disableDragAndDrop()
    }

    /**
     * Shows all the widgets in this list.
     */
    fun showWidgets() {
        showAnimator.start()
    }

    /**
     * Hides all the widgets in this list. Note that this does not remove children in the layout.
     */
    fun hideWidgets() {
        hideAnimator.start()
    }

    fun exitEditMode() {
        widgetViewHolderInEditMode?.isEditing = false
    }

    override fun onWidgetRemoved(removedWidget: AddedWidget) {
        onWidgetListChangedListener?.onRequestRemoveWidget(removedWidget)
    }

    override fun onWidgetResizeStarted() {
        disableDragAndDrop()
    }

    override fun onWidgetResized(widget: AddedWidget, newHeight: Int) {
        enableDragAndDrop()
        onWidgetListChangedListener?.onWidgetResized(widget, newHeight)
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        val widgetsPanel = bindingRegister.mainScreenBinding.widgetsPanel
        val scrollY = widgetsPanel.scrollY

        return if (scrollY == 0 && !widgetsPanel.isEditModeEnabled)
            when (e?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = e.y
                    isClick = true
                    HANDLED
                }

                MotionEvent.ACTION_MOVE ->
                    when {
                        initialY == null -> {
                            initialY = e.y
                            isClick = true
                            HANDLED
                        }

                        abs(e.y - initialY!!) > SCROLL_THRESHOLD &&
                                ((widgetsPanel.isExpanded && e.y - initialY!! > 0) || !widgetsPanel.isExpanded) -> {
                            isClick = false
                            bindingRegister.mainScreenBinding.widgetsPanel.onTouchEvent(e)
                        }

                        !widgetsPanel.isExpanded -> false

                        else -> {
                            super.onTouchEvent(e)
                        }
                    }

                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        super.onTouchEvent(e)
                    } else {
                        initialY = null
                        bindingRegister.mainScreenBinding.widgetsPanel.onTouchEvent(e)
                    }
                }

                MotionEvent.ACTION_BUTTON_PRESS -> {
                    performClick()
                }

                else -> super.onTouchEvent(e)
            }
        else super.onTouchEvent(e)
    }

    private fun onWidgetLongPressed(viewHolder: ViewHolder?) {
        if (viewHolder is WidgetListAdapterItem) {
            // unselect currently selected widget
            widgetViewHolderInEditMode?.isEditing = false
            // enable editing of the newly selected widget
            viewHolder.isEditing = true
            widgetViewHolderInEditMode = viewHolder
        }
    }

    class DiffCallback(
        private val oldList: List<AddedWidget>,
        private val newList: List<AddedWidget>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
