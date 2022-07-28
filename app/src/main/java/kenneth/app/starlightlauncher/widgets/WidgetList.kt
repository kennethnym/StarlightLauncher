package kenneth.app.starlightlauncher.widgets

import android.animation.ObjectAnimator
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.util.BindingRegister
import kenneth.app.starlightlauncher.api.util.activity
import kenneth.app.starlightlauncher.views.ReorderableList
import kenneth.app.starlightlauncher.views.WidgetListAdapter
import kenneth.app.starlightlauncher.views.WidgetListAdapterItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs

private const val ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET =
    "ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET"

private const val ACTIVITY_RESULT_REGISTRY_KEY_CONFIGURE_WIDGET =
    "ACTIVITY_RESULT_REGISTRY_KEY_CONFIGURE_WIDGET"

private const val SCROLL_THRESHOLD = 10

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
internal class WidgetList(context: Context, attrs: AttributeSet) : ReorderableList(context, attrs) {
    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var launcher: StarlightLauncherApi

    @Inject
    lateinit var launcherEventChannel: LauncherEventChannel

    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    private val showAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        duration = 200
    }

    private val hideAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
        duration = 200
    }

    private val requestBindWidgetLauncher: ActivityResultLauncher<Intent>?
    private val configureWidgetActivityLauncher: ActivityResultLauncher<Intent>?

    private val addedWidgets: MutableList<AddedWidget>

    /**
     * Maps app widget IDs to the corresponding [AddedWidget.AndroidWidget]
     */
    private val appWidgetIdMap = mutableMapOf<Int, AddedWidget.AndroidWidget>()

    /**
     * Maps names of providers of app widgets to their index in this widget list.
     */
    private val widgetIndices = mutableMapOf<String, Int>()

    private val widgetListAdapter: WidgetListAdapter

    private var initialY: Float? = null

    private var isClick = false

    /**
     * The widget view holder currently in edit mode.
     */
    private var widgetViewHolderInEditMode: WidgetListAdapterItem? = null

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        isNestedScrollingEnabled = true
        clipToPadding = false
        updatePadding(top = context.resources.getDimensionPixelSize(R.dimen.widget_list_space_between))

        addedWidgets = widgetPreferenceManager.addedWidgets.toMutableList().onEach {
            if (it is AddedWidget.AndroidWidget) {
                appWidgetIdMap[it.appWidgetId] = it
            }
        }
        requestBindWidgetLauncher = activity?.activityResultRegistry?.register(
            ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET,
            ActivityResultContracts.StartActivityForResult(),
            ::onRequestBindWidgetResult
        )
        configureWidgetActivityLauncher = activity?.activityResultRegistry?.register(
            ACTIVITY_RESULT_REGISTRY_KEY_CONFIGURE_WIDGET,
            ActivityResultContracts.StartActivityForResult(),
            ::onConfigureWidgetResult
        )

        layoutManager = LinearLayoutManager(context)
        adapter = WidgetListAdapter(context, addedWidgets).also { widgetListAdapter = it }

        CoroutineScope(mainDispatcher).launch {
            launcherEventChannel.subscribe {
                when (it) {
                    is WidgetPreferenceChanged.NewAndroidWidgetAdded -> {
                        onAndroidWidgetAdded(it.addedWidget, it.appWidgetProviderInfo)
                    }

                    else -> {}
                }
            }
        }

        addOnOrderChangedListener(::onWidgetOrderChanged)
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
        widgetViewHolderInEditMode?.binding?.isEditing = false
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        val widgetsPanel = BindingRegister.activityMainBinding.widgetsPanel
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
                            BindingRegister.activityMainBinding.widgetsPanel.onTouchEvent(e)
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
                        BindingRegister.activityMainBinding.widgetsPanel.onTouchEvent(e)
                    }
                }

                MotionEvent.ACTION_BUTTON_PRESS -> {
                    performClick()
                }

                else -> super.onTouchEvent(e)
            }
        else super.onTouchEvent(e)
    }

    private fun onRequestBindWidgetResult(result: ActivityResult?) {
        val data = result?.data ?: return
        val extras = data.extras
        val appWidgetId =
            extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: return
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                appWidgetIdMap[appWidgetId]?.let {
                    configureWidget(it, appWidgetProviderInfo)
                }
            }
            Activity.RESULT_CANCELED -> {
                appWidgetIdMap.remove(appWidgetId)
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                widgetPreferenceManager.removeAndroidWidget(appWidgetId)
            }
        }
    }

    private fun onConfigureWidgetResult(result: ActivityResult?) {
        val data = result?.data ?: return
        val extras = data.extras
        val appWidgetId =
            extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: return

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                appWidgetIdMap[appWidgetId]?.let {
                    addAndroidWidget(it)
                }
            }
            Activity.RESULT_CANCELED -> {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun bindWidget(
        widget: AddedWidget.AndroidWidget,
        appWidgetProviderInfo: AppWidgetProviderInfo,
    ) {
        val bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            widget.appWidgetId,
            appWidgetProviderInfo.provider
        )

        if (bindAllowed) {
            configureWidget(widget, appWidgetProviderInfo)
        } else {
            requestBindWidgetPermission(appWidgetProviderInfo, widget.appWidgetId)
        }
    }

    private fun configureWidget(
        widget: AddedWidget.AndroidWidget,
        appWidgetProviderInfo: AppWidgetProviderInfo
    ) {
        if (appWidgetProviderInfo.configure != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).run {
                component = appWidgetProviderInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)

                configureWidgetActivityLauncher?.launch(this)
            }
        } else {
            addAndroidWidget(widget)
        }
    }

    private fun addAndroidWidget(widget: AddedWidget.AndroidWidget) {
        widgetListAdapter.addAndroidWidget(widget)
    }

    private fun requestBindWidgetPermission(
        appWidgetProviderInfo: AppWidgetProviderInfo,
        appWidgetId: Int
    ) {
        requestBindWidgetLauncher?.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetProviderInfo.provider)
        })
    }

    private fun onAndroidWidgetAdded(
        widget: AddedWidget.AndroidWidget,
        appWidgetProviderInfo: AppWidgetProviderInfo
    ) {
        addedWidgets += widget
        val index = addedWidgets.size - 1
        widgetIndices[widget.provider.flattenToString()] = index
        appWidgetIdMap[widget.appWidgetId] = widget
        bindWidget(widget, appWidgetProviderInfo)
    }

    private fun onWidgetOrderChanged(fromPosition: Int, toPosition: Int) {
        widgetPreferenceManager.changeWidgetOrder(fromPosition, toPosition)
    }

    private fun onWidgetLongPressed(viewHolder: ViewHolder?) {
        if (viewHolder is WidgetListAdapterItem) {
            // unselect currently selected widget
            widgetViewHolderInEditMode?.binding?.isEditing = false
            // enable editing of the newly selected widget
            viewHolder.binding.isEditing = true
            widgetViewHolderInEditMode = viewHolder
        }
    }
}
