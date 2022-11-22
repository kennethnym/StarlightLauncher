package kenneth.app.starlightlauncher.mediacontrol.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.api.compose.pref.SwitchSettingsListItem

@Composable
fun MediaControlSettingsScreen(
    viewModel: MediaControlSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    fun openNotificationListenerSettings() {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    SettingsScreen(
        title = stringResource(R.string.media_control_title),
        description = stringResource(R.string.media_control_summary),
    ) {
        SettingsList {
            SwitchSettingsListItem(
                icon = painterResource(R.drawable.ic_play_circle),
                title = stringResource(R.string.enable_media_control_title),
                summary = stringResource(R.string.enable_media_control_summary),
                checked = viewModel.isMediaControlEnabled,
                onCheckedChange = {
                    if (it) viewModel.enableMediaControl()
                    else viewModel.disableMediaControl()
                }
            )

            SettingsListItem(
                title = stringResource(R.string.media_control_notification_listener_perm_title),
                summary = stringResource(R.string.media_control_notification_listener_perm_summary),
                onTap = { openNotificationListenerSettings() }
            )
        }
    }
}
