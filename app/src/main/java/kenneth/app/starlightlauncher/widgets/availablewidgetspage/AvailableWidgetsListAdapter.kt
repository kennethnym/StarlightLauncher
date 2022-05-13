package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsListHeaderBinding
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager

private const val ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET =
    "ACTIVITY_RESULT_REGISTRY_KEY_REQUEST_BIND_WIDGET"

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface AvailableWidgetListAdapterEntryPoint {
    fun appearancePreferenceManager(): AppearancePreferenceManager

    fun widgetPreferenceManager(): WidgetPreferenceManager

    fun appWidgetHost(): AppWidgetHost

    fun launcherApi(): StarlightLauncherApi
}

/**
 * Shows available widgets on the phone to an expandable list.
 * Also handles initial creation/configuration of widgets.
 */
internal class AvailableWidgetsListAdapter(
    private val context: Context,
) : BaseExpandableListAdapter() {
    private var providerPackageNames = mutableListOf<String>()
    private var providers: Map<String, List<AppWidgetProviderInfo>> = emptyMap()
    private var appLabels: Map<String, String> = emptyMap()
    private var appIcons: Map<String, Drawable> = emptyMap()
    private var appInfos = emptyMap<String, ApplicationInfo>()

    // some drawables for list items
    private val groupIndicatorExpanded: Drawable?
    private val groupIndicatorEmpty: Drawable?

    private val appearancePreferenceManager: AppearancePreferenceManager
    private val widgetPreferenceManager: WidgetPreferenceManager
    private val launcher: StarlightLauncherApi
    private val appWidgetHost: AppWidgetHost

    init {
        val groupIndicatorSize =
            context.resources.getDimensionPixelSize(R.dimen.available_widgets_list_group_indicator_size)

        val indicatorColor = TypedValue().run {
            context.theme.resolveAttribute(android.R.attr.textColor, this, true)
            data
        }

        groupIndicatorExpanded =
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_angle_up, context.theme)
                ?.apply {
                    setTint(indicatorColor)
                    setBounds(0, 0, groupIndicatorSize, groupIndicatorSize)
                }

        groupIndicatorEmpty =
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_angle_down, context.theme)
                ?.apply {
                    setTint(indicatorColor)
                    setBounds(0, 0, groupIndicatorSize, groupIndicatorSize)
                }

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AvailableWidgetListAdapterEntryPoint::class.java
        ).run {
            appearancePreferenceManager = appearancePreferenceManager()
            widgetPreferenceManager = widgetPreferenceManager()
            launcher = launcherApi()
            appWidgetHost = appWidgetHost()
        }
    }

    override fun getGroupCount(): Int = providers.size

    override fun getChildrenCount(groupPosition: Int): Int {
        val providerPackageName = providerPackageNames[groupPosition]
        return providers[providerPackageName]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        val providerPackageName = providerPackageNames[groupPosition]
        return providers[providerPackageName]!!
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val providerPackageName = providerPackageNames[groupPosition]
        return providers[providerPackageName]!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return "$groupPosition$childPosition".toLong()
    }

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val packageName = providerPackageNames[groupPosition]
        val icon = appearancePreferenceManager.iconPack.getIconOf(appInfos[packageName]!!)
        val iconSize =
            context.resources.getDimensionPixelSize(R.dimen.available_widgets_list_app_icon_size)
        val iconPaddingEnd =
            context.resources.getDimensionPixelSize(R.dimen.available_widgets_list_app_icon_padding_end)
        val indicator = if (isExpanded) groupIndicatorExpanded else groupIndicatorEmpty
        val label = appLabels[packageName]

        return if (convertView == null || convertView !is TextView)
            LayoutInflater.from(parent.context)
                .inflate(R.layout.available_widgets_list_widget_category_header, null).run {
                    findViewById(R.id.widget_provider_app_label)
                }
        else {
            convertView
        }.apply {
            text = label
            setCompoundDrawablesRelative(
                icon.toDrawable(context.resources).apply {
                    setBounds(0, 0, iconSize, iconSize)
                },
                null,
                indicator,
                null
            )
            compoundDrawablePadding = iconPaddingEnd
        }
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val packageName = providerPackageNames[groupPosition]
        val appWidgetProviderInfo = providers[packageName]!![childPosition]
        val widgetPreview = appWidgetProviderInfo.loadPreviewImage(
            context,
            context.resources.displayMetrics.densityDpi
        )
        val widgetPreviewSpacing =
            context.resources.getDimensionPixelSize(R.dimen.available_widgets_list_preview_padding_bottom)

        return if (convertView == null || convertView !is TextView) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.available_widgets_list_widget_provider_item, null).run {
                    findViewById(R.id.widget_provider_name)
                }
        } else {
            convertView
        }.apply {
            text = appWidgetProviderInfo.loadLabel(context.packageManager)
            isClickable = true
            isFocusable = true
            compoundDrawablePadding = widgetPreviewSpacing

            setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                widgetPreview
                    ?: appearancePreferenceManager.iconPack.getIconOf(appInfos[packageName]!!)
                        .toDrawable(context.resources),
                null,
                null,
            )
            setOnClickListener { addSelectedWidget(appWidgetProviderInfo) }
        }
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    @SuppressLint("NotifyDataSetChanged")
    fun showAvailableWidgets(
        providers: Map<String, List<AppWidgetProviderInfo>>,
        infos: Map<String, ApplicationInfo>,
        labels: Map<String, String>,
        icons: Map<String, Drawable>
    ) {
        this.providers = providers.onEach { (packageName, _) ->
            providerPackageNames += packageName
        }
        appInfos = infos
        appLabels = labels
        appIcons = icons
        notifyDataSetChanged()
    }

    private fun addSelectedWidget(appWidgetProviderInfo: AppWidgetProviderInfo) {
        widgetPreferenceManager.addAndroidWidget(
            appWidgetHost.allocateAppWidgetId(),
            appWidgetProviderInfo
        )
        launcher.closeOverlay()
    }
}

internal sealed class AvailableWidgetListItem(rootView: View) : RecyclerView.ViewHolder(rootView)

private class ListHeader(binding: AvailableWidgetsListHeaderBinding) :
    AvailableWidgetListItem(binding.root)
