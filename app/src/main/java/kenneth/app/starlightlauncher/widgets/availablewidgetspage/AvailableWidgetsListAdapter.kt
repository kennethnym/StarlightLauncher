package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.annotation.SuppressLint
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsListHeaderBinding
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import javax.inject.Inject

class AvailableWidgetsListAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appearancePreferenceManager: AppearancePreferenceManager
) : BaseExpandableListAdapter() {
    private var providerPackageNames = mutableListOf<String>()
    private var providers: Map<String, List<AppWidgetProviderInfo>> = emptyMap()
    private var appLabels: Map<String, String> = emptyMap()
    private var appIcons: Map<String, Drawable> = emptyMap()
    private var appInfos = emptyMap<String, ApplicationInfo>()

    // some drawables for list items
    private val groupIndicatorExpanded: Drawable?
    private val groupIndicatorEmpty: Drawable?

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
                .inflate(R.layout.available_widgets_list_widget_category_header, null).apply {
                    findViewById<TextView>(R.id.widget_provider_app_label).apply {
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
        else {
            convertView.apply {
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
        val widgetPreview = ResourcesCompat.getDrawableForDensity(
            context.packageManager.getResourcesForApplication(packageName),
            appWidgetProviderInfo.previewImage,
            context.resources.displayMetrics.densityDpi,
            context.theme,
        )
        val widgetPreviewSpacing =
            context.resources.getDimensionPixelSize(R.dimen.available_widgets_list_preview_padding_bottom)
        return if (convertView == null || convertView !is TextView) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.available_widgets_list_widget_provider_item, null).apply {
                    findViewById<TextView>(R.id.widget_provider_name).apply {
                        text = appWidgetProviderInfo.loadLabel(context.packageManager)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null,
                            widgetPreview,
                            null,
                            null,
                        )
                        compoundDrawablePadding = widgetPreviewSpacing
                    }
                }
        } else {
            convertView.apply {
                text = appWidgetProviderInfo.loadLabel(context.packageManager)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    widgetPreview,
                    null,
                    null,
                )
                compoundDrawablePadding = widgetPreviewSpacing
            }
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
        this.providers = providers.also {
            it.forEach { (packageName, _) ->
                providerPackageNames += packageName
            }
        }
        appInfos = infos
        appLabels = labels
        appIcons = icons
        notifyDataSetChanged()
    }
}

sealed class AvailableWidgetListItem(rootView: View) : RecyclerView.ViewHolder(rootView)

private class ListHeader(binding: AvailableWidgetsListHeaderBinding) :
    AvailableWidgetListItem(binding.root)
