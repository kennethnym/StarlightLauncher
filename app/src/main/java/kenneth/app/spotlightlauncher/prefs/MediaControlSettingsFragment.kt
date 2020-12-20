package kenneth.app.spotlightlauncher.prefs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.spotlightlauncher.R

class MediaControlSettingsFragment : PreferenceFragmentCompat() {
    private val notificationListenerSettingsIntent = Intent(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        else
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.media_control_preferences, rootKey)

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