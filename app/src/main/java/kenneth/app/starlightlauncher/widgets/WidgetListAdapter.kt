package kenneth.app.starlightlauncher.views

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.NOT_HANDLED
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.WidgetFrameBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.utils.dp
import kenneth.app.starlightlauncher.utils.px
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetList
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlin.math.max

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetListAdapterEntryPoint {
    fun launcherApi(): StarlightLauncherApi

    fun extensionManager(): ExtensionManager

    fun appWidgetHost(): AppWidgetHost

    fun widgetPreferenceManager(): WidgetPreferenceManager
}

class WidgetListAdapter(
    private val context: Context,
    val widgets: List<AddedWidget>,
) : RecyclerView.Adapter<WidgetListAdapterItem>() {
    private val addedWidgets = widgets.toMutableList()

    private val extensionManager: ExtensionManager

    private val launcherApi: StarlightLauncherApi

    private val appWidgetHost: AppWidgetHost

    private val widgetPreferenceManager: WidgetPreferenceManager

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private var widgetList: WidgetList? = null

    private val appWidgetIds: MutableList<Int?> =
        addedWidgets
            .map { null }
            .toMutableList()

    init {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetListAdapterEntryPoint::class.java
        ).run {
            extensionManager = extensionManager()
            launcherApi = launcherApi()
            appWidgetHost = appWidgetHost()
            widgetPreferenceManager = widgetPreferenceManager()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetListAdapterItem {
        val binding =
            WidgetFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
                isEditing = false
            }
        return WidgetListAdapterItem(context, binding, widgetList)
    }

    override fun onBindViewHolder(holder: WidgetListAdapterItem, position: Int) {
        when (val addedWidget = addedWidgets[position]) {
            is AddedWidget.StarlightWidget -> {
                showStarlightWidget(addedWidget, holder, position)
            }
            is AddedWidget.AndroidWidget -> {
                showAndroidWidget(addedWidget, holder, position)
            }
        }
    }

    override fun getItemCount(): Int = addedWidgets.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (recyclerView is WidgetList) {
            widgetList = recyclerView
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        widgetList = null
    }

    /**
     * Adds the given [AddedWidget.AndroidWidget] to the end of this list.
     */
    fun addAndroidWidget(widget: AddedWidget.AndroidWidget) {
        val widgetIndex = addedWidgets.size
        addedWidgets += widget
        appWidgetIds += null
        notifyItemInserted(widgetIndex)
    }

    private fun showAndroidWidget(
        addedWidget: AddedWidget.AndroidWidget,
        holder: WidgetListAdapterItem,
        position: Int,
    ) {
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(addedWidget.appWidgetId)

        holder.addedWidget = addedWidget
        appWidgetHost.createView(
            context.applicationContext,
            addedWidget.appWidgetId,
            appWidgetProviderInfo
        )
            .run {
                setAppWidget(appWidgetId, appWidgetInfo)
                holder.binding.widgetFrameContainer.layoutParams =
                    holder.binding.widgetFrameContainer.layoutParams.apply {
                        height = addedWidget.height.px
                    }
                holder.binding.widgetFrame.addView(this)
            }

        holder.binding.removeWidgetBtn.setOnClickListener {
            removeAndroidWidget(appWidgetProviderInfo, position)
        }
    }

    private fun showStarlightWidget(
        widget: AddedWidget.StarlightWidget,
        holder: WidgetListAdapterItem,
        position: Int,
    ) {
        extensionManager.lookupWidget(widget.extensionName)?.let { creator ->
            creator.createWidget(holder.binding.widgetFrame, launcherApi).also {
                holder.binding.widgetFrame.addView(it.rootView)
            }
        }
        holder.binding.removeWidgetBtn.setOnClickListener {
            removeStarlightWidget(widget.extensionName, position)
        }
    }

    private fun removeAndroidWidget(appWidgetProviderInfo: AppWidgetProviderInfo, position: Int) {
        widgetPreferenceManager.removeAndroidWidget(appWidgetProviderInfo)
        addedWidgets.removeAt(position)
        appWidgetIds.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun removeStarlightWidget(extensionName: String, position: Int) {
        widgetPreferenceManager.removeStarlightWidget(extensionName)
        addedWidgets.removeAt(position)
        notifyItemRemoved(position)
    }
}

class WidgetListAdapterItem(
    context: Context,
    val binding: WidgetFrameBinding,
    private val widgetList: WidgetList?
) :
    RecyclerView.ViewHolder(binding.root), View.OnTouchListener {
    private var lastY = 0f

    private var appWidgetProviderInfo: AppWidgetProviderInfo? = null

    private val widgetPreferenceManager: WidgetPreferenceManager

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    var addedWidget: AddedWidget? = null
        set(value) {
            field = value
            if (value is AddedWidget.AndroidWidget) {
                appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(value.appWidgetId)
            }
        }

    init {
        with(binding) {
            widgetResizeHandle.setOnTouchListener(this@WidgetListAdapterItem)
            cancelBtn.setOnClickListener {
                saveWidgetHeight()
                isEditing = false
            }
        }

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetListAdapterEntryPoint::class.java
        ).run {
            widgetPreferenceManager = widgetPreferenceManager()
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
                widgetList?.disableDragAndDrop()
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
                        height =
                            if (addedWidget is AddedWidget.AndroidWidget)
                                max(newHeight, appWidgetProviderInfo?.minHeight ?: newHeight)
                            else newHeight
                    }

                lastY = currentY

                HANDLED
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // resizing widget done, re-enable drag and drop.
                widgetList?.enableDragAndDrop()
                HANDLED
            }

            else -> {
                NOT_HANDLED
            }
        }
    }

    private fun saveWidgetHeight() {
        addedWidget?.let {
            widgetPreferenceManager.changeWidgetHeight(
                it,
                binding.widgetFrameContainer.height.dp
            )
        }
    }
}
