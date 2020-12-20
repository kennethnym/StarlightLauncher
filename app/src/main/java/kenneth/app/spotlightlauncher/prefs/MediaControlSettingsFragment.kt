package kenneth.app.spotlightlauncher.prefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
class MediaControlSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val notificationListenerSettingsIntent = Intent(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        else
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
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
    }

    private fun openNotificationListenerSettings() {
        startActivity(notificationListenerSettingsIntent)
    }
}