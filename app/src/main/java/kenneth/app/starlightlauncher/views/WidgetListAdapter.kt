package kenneth.app.starlightlauncher.views

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.WidgetFrameBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager

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
) : RecyclerView.Adapter<WidgetListAdapterItem>() {
    private val extensionManager: ExtensionManager

    private val launcherApi: StarlightLauncherApi

    private val appWidgetHost: AppWidgetHost

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    private val appWidgetProviders = appWidgetManager.installedProviders
        .fold(mutableMapOf<String, AppWidgetProviderInfo>()) { m, info ->
            m.apply {
                put(info.provider.flattenToString(), info)
            }
        }

    private val addedWidgets: MutableList<AddedWidget>

    private val widgetViews: MutableList<View?>

    init {
        val hilt = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetListAdapterEntryPoint::class.java
        )

        extensionManager = hilt.extensionManager()
        launcherApi = hilt.launcherApi()
        appWidgetHost = hilt.appWidgetHost()
        addedWidgets = hilt.widgetPreferenceManager().addedWidgets.toMutableList()
        widgetViews = MutableList(addedWidgets.size) { null }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetListAdapterItem {
        val binding = WidgetFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WidgetListAdapterItem(binding)
    }

    override fun onBindViewHolder(holder: WidgetListAdapterItem, position: Int) {
        widgetViews[position]
            ?.let {
                holder.binding.widgetFrame.addView(it)
            }
            ?: when (val addedWidget = addedWidgets[position]) {
                is AddedWidget.StarlightWidget -> {
                    extensionManager.lookupWidget(addedWidget.extensionName)?.let { creator ->
                        creator.createWidget(holder.binding.widgetFrame, launcherApi).also {
                            widgetViews[position] = it.rootView
                            holder.binding.widgetFrame.addView(it.rootView)
                        }
                    }
                }
                is AddedWidget.AndroidWidget -> {
                    appWidgetProviders[addedWidget.provider.flattenToString()]?.let {
                        val appWidgetId = appWidgetHost.allocateAppWidgetId()
                        appWidgetHost.createView(context, appWidgetId, it)
                            .also { appWidgetHostView ->
                                widgetViews[position] = appWidgetHostView
                                holder.binding.widgetFrame.addView(appWidgetHostView)
                            }
                    }
                }
            }
    }

    override fun getItemCount(): Int = addedWidgets.size

    fun addAndroidWidget(widget: AddedWidget.AndroidWidget) {
        val widgetIndex = addedWidgets.size
        widgetViews += null
        addedWidgets += widget
        notifyItemInserted(widgetIndex)
    }
}

class WidgetListAdapterItem(val binding: WidgetFrameBinding) : RecyclerView.ViewHolder(binding.root)
