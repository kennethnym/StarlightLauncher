package kenneth.app.starlightlauncher.setup.feature

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.databinding.FragmentSetupFeatureBinding
import kenneth.app.starlightlauncher.databinding.SetupFeatureItemBinding

/**
 * This setup step allows users to enable features they want.
 */
internal class FeatureFragment : Fragment() {
    private var sharedPreferences: SharedPreferences? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onDetach() {
        super.onDetach()
        sharedPreferences = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSetupFeatureBinding.inflate(inflater).run {
        AVAILABLE_FEATURES.forEach {
            val featurePrefKey = getString(it.key)

            SetupFeatureItemBinding.inflate(inflater, root, true).apply {
                feature = it
                enableFeatureCheckbox.isChecked =
                    sharedPreferences?.getBoolean(featurePrefKey, false) ?: false

                root.setOnClickListener { _ -> toggleFeature(getString(it.key), this) }
                enableFeatureCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    setFeatureEnabled(featurePrefKey, enabled = isChecked)
                }
            }
        }

        root
    }

    private fun setFeatureEnabled(prefKey: String, enabled: Boolean) {
        sharedPreferences?.edit(commit = true) {
            putBoolean(prefKey, enabled)
        }
    }

    private fun toggleFeature(prefKey: String, binding: SetupFeatureItemBinding) {
        binding.enableFeatureCheckbox.isChecked = !binding.enableFeatureCheckbox.isChecked
        val isFeatureEnabled = binding.enableFeatureCheckbox.isChecked
        setFeatureEnabled(prefKey, isFeatureEnabled)
    }
}
