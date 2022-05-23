package kenneth.app.starlightlauncher.setup.feature

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.databinding.FeatureItemBinding
import kenneth.app.starlightlauncher.databinding.FragmentSetupFeatureBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
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
    ): View = FragmentSetupFeatureBinding.inflate(inflater).run {
        AVAILABLE_FEATURES.forEach {
            val featurePrefKey = getString(it.key)

            FeatureItemBinding.inflate(inflater, root, true).apply {
                name = getString(it.name)
                description = getString(it.description)
                enableFeatureCheckbox.isChecked =
                    sharedPreferences.getBoolean(featurePrefKey, false)

                root.setOnClickListener { _ -> toggleFeature(getString(it.key), this) }
                enableFeatureCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    setFeatureEnabled(featurePrefKey, enabled = isChecked)
                }
            }
        }

        extensionManager.installedWidgets.forEach {
            FeatureItemBinding.inflate(inflater, root, true).apply {
                name = it.metadata.displayName
                description = it.metadata.description

                enableFeatureCheckbox.isChecked =
                    widgetPreferenceManager.isStarlightWidgetAdded(it.metadata.extensionName)

                root.setOnClickListener { _ ->
                    enableFeatureCheckbox.isChecked = !enableFeatureCheckbox.isChecked
                    toggleWidget(it, enableFeatureCheckbox.isChecked)
                }
                enableFeatureCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    toggleWidget(it, isChecked)
                }
            }
        }

        root
    }

    private fun setFeatureEnabled(prefKey: String, enabled: Boolean) {
        sharedPreferences.edit(commit = true) {
            putBoolean(prefKey, enabled)
        }
    }

    private fun toggleWidget(widget: WidgetCreator, enabled: Boolean) {
        if (enabled) {
            widgetPreferenceManager.addStarlightWidget(widget.metadata.extensionName)
        } else {
            widgetPreferenceManager.removeStarlightWidget(widget.metadata.extensionName)
        }
    }

    private fun toggleFeature(prefKey: String, binding: FeatureItemBinding) {
        binding.enableFeatureCheckbox.isChecked = !binding.enableFeatureCheckbox.isChecked
        val isFeatureEnabled = binding.enableFeatureCheckbox.isChecked
        setFeatureEnabled(prefKey, isFeatureEnabled)
    }
}
