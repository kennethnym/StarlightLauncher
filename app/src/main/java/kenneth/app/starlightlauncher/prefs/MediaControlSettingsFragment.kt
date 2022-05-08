package kenneth.app.starlightlauncher.prefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
internal class MediaControlSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val notificationListenerSettingsIntent = Intent(
        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.media_control_preferences, rootKey)

        findPreference<SwitchPreferenceCompat>(getString(R.string.media_control_enabled))
            ?.isChecked =
            sharedPreferences.getBoolean(getString(R.string.media_control_enabled), true)

        findPreference<Preference>(getString(R.string.media_control_notification_listener_permission))
            ?.setOnPreferenceClickListener {
                openNotificationListenerSettings()
                true
            }

        changeToolbarTitle()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.media_control_title)
    }

    private fun openNotificationListenerSettings() {
        startActivity(notificationListenerSettingsIntent)
    }
}