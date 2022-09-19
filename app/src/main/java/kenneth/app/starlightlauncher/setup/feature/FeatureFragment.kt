package kenneth.app.starlightlauncher.setup.feature

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.databinding.FeatureItemBinding
import kenneth.app.starlightlauncher.databinding.FragmentSetupFeatureBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This setup step allows users to enable features they want.
 */
@AndroidEntryPoint
internal class FeatureFragment : Fragment() {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        FragmentSetupFeatureBinding.inflate(inflater).run {
            AVAILABLE_FEATURES.forEach { feature ->
                FeatureItemBinding.inflate(inflater, root, true).apply {
                    name = getString(feature.name)
                    description = getString(feature.description)

                    root.setOnClickListener {
                        enableFeatureCheckbox.isChecked = !enableFeatureCheckbox.isChecked
                    }
                    enableFeatureCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        setFeatureEnabled(feature.key, enabled = isChecked)
                    }

                    lifecycleScope.launch {
                        context.dataStore.data.map {
                            it[feature.key] ?: context.resources.getBoolean(feature.defaultEnabled)
                        }.collect {
                            enableFeatureCheckbox.isChecked = it
                        }
                    }
                }
            }

            extensionManager.installedWidgets.forEach {
                FeatureItemBinding.inflate(inflater, root, true).apply {
                    name = it.metadata.displayName
                    description = it.metadata.description

                    enableFeatureCheckbox.isChecked =
                        widgetPreferenceManager.isStarlightWidgetAdded(it.metadata.extensionName)

                    root.setOnClickListener {
                        enableFeatureCheckbox.isChecked = !enableFeatureCheckbox.isChecked
                    }
                    enableFeatureCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        toggleWidget(it, isChecked)
                    }
                }
            }

            root
        }
    }

    private fun setFeatureEnabled(prefKey: Preferences.Key<Boolean>, enabled: Boolean) {
        lifecycleScope.launch {
            context?.dataStore?.edit {
                it[prefKey] = enabled
            }
        }
    }

    private fun toggleWidget(widget: WidgetCreator, enabled: Boolean) {
        if (enabled) {
            widgetPreferenceManager.addStarlightWidget(widget.metadata.extensionName)
        } else {
            widgetPreferenceManager.removeStarlightWidget(widget.metadata.extensionName)
        }
    }
}
