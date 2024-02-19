package kenneth.app.starlightlauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.NOT_HANDLED
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.WidgetFrameBinding
import kenneth.app.starlightlauncher.util.toDp
import kenneth.app.starlightlauncher.util.toPx
import kotlin.math.max

private const val VIEW_TYPE_ANDROID_WIDGET = 0
private const val VIEW_TYPE_STARLIGHT_WIDGET = 1

/**
 * Adapter for [WidgetListView]
 */
internal class WidgetListAdapter(
    context: Context,
    var widgets: List<AddedWidget>,
    private val appWidgetHost: AppWidgetHost,
    private val eventListener: WidgetListEventListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface WidgetListEventListener {
        fun onWidgetRemoved(removedWidget: AddedWidget)

        fun onWidgetResizeStarted()

        fun onWidgetResized(widget: AddedWidget, newHeight: Int)
    }

    private val context = context.applicationContext

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private var widgetListView: WidgetListView? = null

    /**
     * IDs of [AddedWidget]s that are in layout.
     */
    private var widgetsInLayout = mutableSetOf<Int>()

    override fun getItemViewType(position: Int): Int = when (widgets[position]) {
        is AddedWidget.AndroidWidget -> VIEW_TYPE_ANDROID_WIDGET
        is AddedWidget.StarlightWidget -> VIEW_TYPE_STARLIGHT_WIDGET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            WidgetFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
                isEditing = false
            }

        return when (viewType) {
            VIEW_TYPE_ANDROID_WIDGET -> AndroidWidgetListAdapterItem(binding, eventListener)
            VIEW_TYPE_STARLIGHT_WIDGET -> StarlightWidgetListAdapterItem(binding)
            else -> throw Error("View type $viewType not recognized by WidgetListAdapter.")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val widget = widgets[position]
        when {
            widget is AddedWidget.AndroidWidget && holder is AndroidWidgetListAdapterItem -> {
                showAndroidWidget(widget, holder)
            }

            widget is AddedWidget.StarlightWidget && holder is StarlightWidgetListAdapterItem -> {
                showStarlightWidget(widget, holder)
            }
        }
    }

    override fun getItemCount(): Int = widgets.size

    override fun getItemId(position: Int): Long = widgets[position].id.toLong()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (recyclerView is WidgetListView) {
            widgetListView = recyclerView
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        widgetListView = null
    }

    private fun showAndroidWidget(
        addedWidget: AddedWidget.AndroidWidget,
        holder: AndroidWidgetListAdapterItem,
    ) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(addedWidget.appWidgetId)

        holder.apply {
            this.addedWidget = addedWidget
            this.appWidgetInfo = appWidgetInfo
        }

        appWidgetHost.createView(
            context.applicationContext,
            addedWidget.appWidgetId,
            appWidgetInfo
        )
            .run {
                setAppWidget(appWidgetId, appWidgetInfo)
                if (this is LauncherAppWidgetHostView) {
                    scrollingParent = widgetListView
                }

                holder.binding.widgetFrameContainer.layoutParams =
                    holder.binding.widgetFrameContainer.layoutParams.apply {
                        height = addedWidget.height.toPx()
                    }

                holder.binding.widgetFrame.updatePadding(
                    top = context.resources.getDimensionPixelSize(R.dimen.widget_list_space_between),
                    bottom = context.resources.getDimensionPixelSize(R.dimen.widget_list_space_between)
                )

                holder.binding.widgetFrame.addView(this)
            }

        holder.binding.apply {
            resizable = true
            removeWidgetBtn.setOnClickListener {
                eventListener.onWidgetRemoved(addedWidget)
            }
            cancelBtn.setOnClickListener {
                isEditing = false
            }
        }

        widgetsInLayout.add(addedWidget.id)
    }

    private fun showStarlightWidget(
        widget: AddedWidget.StarlightWidget,
        holder: StarlightWidgetListAdapterItem,
    ) {
        widget.widgetCreator?.let { creator ->
            creator.createWidget(holder.binding.widgetFrame).also {
                holder.binding.widgetFrame.addView(it.rootView)
            }
        }

        holder.binding.apply {
            resizable = false
            removeWidgetBtn.setOnClickListener {
                eventListener.onWidgetRemoved(widget)
            }
            cancelBtn.setOnClickListener {
                isEditing = false
            }
        }

        widgetsInLayout.add(widget.id)
    }
}

/**
 * All view holders in [WidgetListAdapter] must implement this interface.
 */
interface WidgetListAdapterItem {
    /**
     * Whether this widget is being edited.
     */
    var isEditing: Boolean
}

internal class AndroidWidgetListAdapterItem(
    val binding: WidgetFrameBinding,
    private val eventListener: WidgetListAdapter.WidgetListEventListener
) :
    RecyclerView.ViewHolder(binding.root),
    WidgetListAdapterItem,
    View.OnTouchListener {
    var appWidgetInfo: AppWidgetProviderInfo? = null

    var addedWidget: AddedWidget.AndroidWidget? = null

    override var isEditing: Boolean = false
        set(value) {
            field = value
            binding.isEditing = value
        }

    private var lastY = 0f

    init {
        with(binding) {
            widgetResizeHandle.setOnTouchListener(this@AndroidWidgetListAdapterItem)
            cancelBtn.setOnClickListener {
                saveWidgetHeight()
                isEditing = false
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // request parent to not intercept touch event from this
                // so that gesture of resizing widget doesn't conflict with parent
                binding.root.parent.requestDisallowInterceptTouchEvent(true)
                // disables drag and drop of [WidgetList] temporarily so that drag-n-drop
                // gesture will not conflict with resizing widget.
                eventListener.onWidgetResizeStarted()
                lastY = event.rawY
                HANDLED
            }

            MotionEvent.ACTION_MOVE -> {
                // request parent to not intercept touch event from this
                // so that gesture of resizing widget doesn't conflict with parent
                binding.root.parent.requestDisallowInterceptTouchEvent(true)
                val currentY = event.rawY
                val delta = currentY - lastY
                val currentHeight = binding.widgetFrameContainer.height

                binding.widgetFrameContainer.layoutParams =
                    binding.widgetFrameContainer.layoutParams.apply {
                        val newHeight = currentHeight + delta.toInt()
                        height = max(newHeight, appWidgetInfo?.minHeight ?: newHeight)
                    }

                lastY = currentY

                HANDLED
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                saveWidgetHeight()
                HANDLED
            }

            else -> {
                NOT_HANDLED
            }
        }
    }

    private fun saveWidgetHeight() {
        addedWidget?.let {
            eventListener.onWidgetResized(
                it,
                newHeight = binding.widgetFrameContainer.height.toDp()
            )
        }
    }
}

internal class StarlightWidgetListAdapterItem(val binding: WidgetFrameBinding) :
    RecyclerView.ViewHolder(binding.root), WidgetListAdapterItem {
    override var isEditing: Boolean = false
        set(value) {
            field = value
            binding.isEditing = value
        }
}
