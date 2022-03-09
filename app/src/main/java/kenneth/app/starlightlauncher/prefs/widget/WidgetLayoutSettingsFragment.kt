package kenneth.app.starlightlauncher.prefs.widget

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.utils.swap
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.views.ReorderablePreference
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class WidgetLayoutSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    private val widgetOrder = mutableListOf<String>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.widget_layout_preferences, rootKey)

        widgetOrder += widgetPreferenceManager.widgetOrder

        findPreference<ReorderablePreference>(getString(R.string.pref_key_widget_order))
            ?.apply {
                items = widgetOrder.mapNotNull { extName ->
                    extensionManager.getWidgetMetadata(extName)?.let {
                        ReorderablePreference.Item(
                            value = extName,
                            title = it.displayName,
                            summary = it.description,
                        )
                    }
                }
                setOnPreferenceOrderChanged(::onWidgetOrderChanged)
            }
    }

    private fun onWidgetOrderChanged(fromPosition: Int, toPosition: Int) {
        widgetOrder.swap(fromPosition, toPosition)
        widgetPreferenceManager.changeWidgetOrder(fromPosition, toPosition, widgetOrder)
    }
}