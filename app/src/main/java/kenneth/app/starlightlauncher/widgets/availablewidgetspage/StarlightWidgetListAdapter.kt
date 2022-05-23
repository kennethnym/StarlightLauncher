package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.databinding.FeatureItemBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface StarlightWidgetListAdapterEntryPoint {
    fun extensionManager(): ExtensionManager

    fun widgetPreferenceManager(): WidgetPreferenceManager
}

internal class StarlightWidgetListAdapter(context: Context) :
    RecyclerView.Adapter<StarlightWidgetListItem>() {
    private val widgets: List<WidgetCreator>

    private val widgetPreferenceManager: WidgetPreferenceManager

    init {
        EntryPointAccessors.fromApplication(
            context,
            StarlightWidgetListAdapterEntryPoint::class.java
        ).run {
            widgets = extensionManager().installedWidgets.toList()
            widgetPreferenceManager = widgetPreferenceManager()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StarlightWidgetListItem {
        val binding = FeatureItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StarlightWidgetListItem(binding)
    }

    override fun onBindViewHolder(holder: StarlightWidgetListItem, position: Int) {
        val widget = widgets[position]

        holder.binding.apply {
            name = widget.metadata.displayName
            description = widget.metadata.description

            enableFeatureCheckbox.apply {
                isChecked =
                    widgetPreferenceManager.isStarlightWidgetAdded(widget.metadata.extensionName)

                root.setOnClickListener {
                    enableFeatureCheckbox.isChecked = !enableFeatureCheckbox.isChecked
                }

                setOnCheckedChangeListener { _, isChecked ->
                    toggleWidget(widget.metadata.extensionName, enabled = isChecked)
                }
            }
        }
    }

    override fun getItemCount(): Int = widgets.size

    private fun toggleWidget(extensionName: String, enabled: Boolean) {
        if (enabled) {
            widgetPreferenceManager.addStarlightWidget(extensionName)
        } else {
            widgetPreferenceManager.removeStarlightWidget(extensionName)
        }
    }

}

internal class StarlightWidgetListItem(val binding: FeatureItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

}