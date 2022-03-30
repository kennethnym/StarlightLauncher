package kenneth.app.starlightlauncher.views

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
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.animations.CardAnimation
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.utils.activity
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceChanged
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.delay
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET =
    "ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET"

private const val ACTIVITY_RESULT_REGISTRY_KEY_CONFIGURE_WIDGET =
    "ACTIVITY_RESULT_REGISTRY_KEY_CONFIGURE_WIDGET"

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
class WidgetList(context: Context, attrs: AttributeSet) : ReorderableList(context, attrs) {
    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var launcher: StarlightLauncherApi

    private val animations: List<CardAnimation>

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
     * Maps names of providers of app widgets to their index in this widget list.
     */
    private val widgetIndices = mutableMapOf<String, Int>()

    private val widgetListAdapter: WidgetListAdapter

    /**
     * The widget view holder currently in edit mode.
     */
    private var widgetViewHolderInEditMode: WidgetListAdapterItem? = null

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        animations = generateAnimations()
        addedWidgets = widgetPreferenceManager.addedWidgets.toMutableList()
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

        widgetPreferenceManager.addOnWidgetPreferenceChangedListener {
            when (it) {
                is WidgetPreferenceChanged.NewAndroidWidgetAdded -> {
                    onAndroidWidgetAdded(it.addedWidget, it.appWidgetProviderInfo)
                }

                else -> {
                }
            }
        }

        addOnOrderChangedListener(::onWidgetOrderChanged)
        addOnSelectionChangedListener(::onWidgetLongPressed)
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadWidgets()
    }

    private fun onRequestBindWidgetResult(result: ActivityResult?) {
        val data = result?.data ?: return
        val extras = data.extras
        val appWidgetId =
            extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: return
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                configureWidget(appWidgetProviderInfo, appWidgetId)
            }
            Activity.RESULT_CANCELED -> {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                widgetPreferenceManager.removeAndroidWidget(appWidgetProviderInfo)
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
                val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                showWidget(appWidgetProviderInfo, appWidgetId)
            }
            Activity.RESULT_CANCELED -> {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun loadWidgets() {
        val appWidgetProviders = appWidgetManager.installedProviders
            .fold(mutableMapOf<String, AppWidgetProviderInfo>()) { m, info ->
                m.apply {
                    put(info.provider.flattenToString(), info)
                }
            }

        addedWidgets.forEachIndexed { i, it ->
            if (it is AddedWidget.AndroidWidget) {
                val providerName = it.provider.flattenToString()
                widgetIndices[providerName] = i
                appWidgetProviders[providerName]?.let { info ->
                    bindWidget(info)
                }
            }
        }
    }

    private fun bindWidget(
        appWidgetProviderInfo: AppWidgetProviderInfo,
        configure: Boolean = false
    ) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        when {
            !appWidgetManager.bindAppWidgetIdIfAllowed(
                appWidgetId,
                appWidgetProviderInfo.provider
            ) -> {
                requestBindWidgetPermission(appWidgetProviderInfo, appWidgetId)
            }

            configure -> {
                configureWidget(appWidgetProviderInfo, appWidgetId)
            }

            else -> showWidget(appWidgetProviderInfo, appWidgetId)
        }
    }

    private fun configureWidget(appWidgetProviderInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        if (appWidgetProviderInfo.configure != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).run {
                component = appWidgetProviderInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                configureWidgetActivityLauncher?.launch(this)
            }
        } else {
            showWidget(appWidgetProviderInfo, appWidgetId)
        }
    }

    private fun showWidget(appWidgetProviderInfo: AppWidgetProviderInfo, appWidgetId: Int) {
        val providerName = appWidgetProviderInfo.provider.flattenToString()
        widgetIndices[providerName]?.let {
            widgetListAdapter.showAndroidWidgetAt(it, appWidgetId)
        }
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
        widgetListAdapter.addAndroidWidget(widget)
        bindWidget(appWidgetProviderInfo, configure = true)
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

    /**
     * Generates card animations for every widget.
     */
    private fun generateAnimations(): List<CardAnimation> =
        children.foldIndexed(mutableListOf()) { i, anims, child ->
            anims.apply { add(CardAnimation(child, i * 20L)) }
        }
}
