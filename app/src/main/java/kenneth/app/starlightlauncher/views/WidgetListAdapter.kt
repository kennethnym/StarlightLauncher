package kenneth.app.starlightlauncher.views

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.WidgetFrameBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import java.lang.IndexOutOfBoundsException

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

    private val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

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
        val hilt = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetListAdapterEntryPoint::class.java
        )

        extensionManager = hilt.extensionManager()
        launcherApi = hilt.launcherApi()
        appWidgetHost = hilt.appWidgetHost()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetListAdapterItem {
        val binding = WidgetFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WidgetListAdapterItem(binding)
    }

    override fun onBindViewHolder(holder: WidgetListAdapterItem, position: Int) {
        when (val addedWidget = addedWidgets[position]) {
            is AddedWidget.StarlightWidget -> {
                showStarlightWidget(addedWidget, destView = holder.binding.widgetFrame)
            }
            is AddedWidget.AndroidWidget -> {
                appWidgetIds[position]?.let {
                    showAndroidWidget(it, destView = holder.binding.widgetFrame)
                }
            }
        }
    }

    override fun getItemCount(): Int = addedWidgets.size

    fun showAndroidWidgetAt(position: Int, appWidgetId: Int) {
        appWidgetIds[position] = appWidgetId
        notifyItemChanged(position)
    }

    fun addAndroidWidget(widget: AddedWidget.AndroidWidget) {
        val widgetIndex = addedWidgets.size
        addedWidgets += widget
        appWidgetIds += null
        notifyItemInserted(widgetIndex)
    }

    private fun showAndroidWidget(
        appWidgetId: Int,
        destView: ViewGroup
    ) {
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        appWidgetHost.createView(context.applicationContext, appWidgetId, appWidgetProviderInfo)
            .run {
                setAppWidget(appWidgetId, appWidgetInfo)
                destView.addView(this)
            }
    }

    private fun showStarlightWidget(
        widget: AddedWidget.StarlightWidget,
        destView: ViewGroup
    ) {
        extensionManager.lookupWidget(widget.extensionName)?.let { creator ->
            creator.createWidget(destView, launcherApi).also {
                destView.addView(it.rootView)
            }
        }
    }
}

class WidgetListAdapterItem(val binding: WidgetFrameBinding) : RecyclerView.ViewHolder(binding.root)
