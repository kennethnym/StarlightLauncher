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
import kenneth.app.starlightlauncher.widgets.AddedWidget
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

    private val appWidgetProviders = appWidgetManager.installedProviders
        .fold(mutableMapOf<String, AppWidgetProviderInfo>()) { m, info ->
            m.apply {
                put(info.provider.flattenToString(), info)
            }
        }

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
        return WidgetListAdapterItem(binding, widgetList)
    }

    override fun onBindViewHolder(holder: WidgetListAdapterItem, position: Int) {
        when (val addedWidget = addedWidgets[position]) {
            is AddedWidget.StarlightWidget -> {
                showStarlightWidget(addedWidget, holder, position)
            }
            is AddedWidget.AndroidWidget -> {
                appWidgetIds[position]?.let {
                    showAndroidWidget(it, holder, position)
                }
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
     * Shows the [AddedWidget.AndroidWidget] added by [addAndroidWidget] at [position].
     * The widget must be bound previously with [appWidgetId].
     */
    fun showAndroidWidgetAt(position: Int, appWidgetId: Int) {
        appWidgetIds[position] = appWidgetId
        notifyItemChanged(position)
    }

    /**
     * Adds the given [AddedWidget.AndroidWidget] to the end of this list. It will not be shown
     * until [showAndroidWidgetAt] is called.
     */
    fun addAndroidWidget(widget: AddedWidget.AndroidWidget) {
        val widgetIndex = addedWidgets.size
        addedWidgets += widget
        appWidgetIds += null
        notifyItemInserted(widgetIndex)
    }

    private fun showAndroidWidget(
        appWidgetId: Int,
        holder: WidgetListAdapterItem,
        position: Int,
    ) {
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        holder.appWidgetProviderInfo = appWidgetProviderInfo
        appWidgetHost.createView(context.applicationContext, appWidgetId, appWidgetProviderInfo)
            .run {
                setAppWidget(appWidgetId, appWidgetInfo)
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

class WidgetListAdapterItem(val binding: WidgetFrameBinding, private val widgetList: WidgetList?) :
    RecyclerView.ViewHolder(binding.root), View.OnTouchListener {
    private var lastY = 0f

    var appWidgetProviderInfo: AppWidgetProviderInfo? = null

    init {
        with(binding) {
            widgetResizeHandle.setOnTouchListener(this@WidgetListAdapterItem)
            cancelBtn.setOnClickListener {
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
                        Log.d("starlight", "newHeight $newHeight")
                        height = max(newHeight, appWidgetProviderInfo?.minHeight ?: newHeight)
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
}
