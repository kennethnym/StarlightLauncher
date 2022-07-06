package kenneth.app.starlightlauncher.prefs.appearance

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
internal class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val storagePermissinoRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onPermissionRequestResult
    )

    private var blurEffectPreference: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_preferences, rootKey)

        findPreference<SwitchPreference>(getString(R.string.pref_key_appearance_blur_effect_enabled))
            ?.also { blurEffectPreference = it }
            ?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean && newValue) checkStoragePermission()
                else true
            }

        updateBlurEffectPreference()
        changeToolbarTitle()
    }

    override fun onResume() {
        super.onResume()
        updateBlurEffectPreference()
        changeToolbarTitle()
    }

    /**
     * Checks if the launcher has the permission to access the wallpaper,
     * if not, turn off blur effect automatically
     */
    private fun updateBlurEffectPreference() {
        val hasPermission =
            context?.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission && appearancePreferenceManager.isBlurEffectEnabled) {
            blurEffectPreference?.isChecked = false
            appearancePreferenceManager.setBlurEffectEnabled(enabled = false)
        } else if (hasPermission) {
            blurEffectPreference?.isChecked = appearancePreferenceManager.isBlurEffectEnabled
        }
    }

    private fun onPermissionRequestResult(isGranted: Boolean) {
        if (isGranted) {
            blurEffectPreference?.isChecked = true
        }
    }

    /**
     * Checks if the launcher has read storage permission, if not, launch the permission prompt.
     *
     * @return Whether the launcher has read storage permission.
     */
    private fun checkStoragePermission(): Boolean {
        val context = this.context ?: return false
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermissinoRequestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            return false
        }
        return true
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.appearance_title)
    }
}
