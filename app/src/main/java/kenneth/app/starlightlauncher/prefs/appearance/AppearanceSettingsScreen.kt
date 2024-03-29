package kenneth.app.starlightlauncher.prefs.appearance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.api.compose.pref.SwitchSettingsListItem
import kenneth.app.starlightlauncher.prefs.SETTINGS_ROUTE_ICON_PACK

@Composable
internal fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: AppearanceSettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionRequestLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.changeIsBlurEffectEnabled(isGranted)
        }

    fun toggleBlurEffect(isEnabled: Boolean) {
        val hasPermission =
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (isEnabled && !hasPermission) {
            permissionRequestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            viewModel.changeIsBlurEffectEnabled(isEnabled)
        }
    }

    SettingsScreen(
        title = stringResource(R.string.appearance_title),
        description = stringResource(R.string.appearance_subtitle),
        gutter = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsListItem(
                icon = painterResource(R.drawable.ic_icons),
                title = stringResource(R.string.appearance_change_icon_pack_title),
                summary = stringResource(R.string.appearance_change_icon_pack_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_ICON_PACK) }
            )

            SwitchSettingsListItem(
                icon = null,
                // WallpaperManager.getDrawable still requires READ_EXTERNAL_STORAGE
                // but upon requesting the permission the system automatically rejects it
                // without prompting the user
                // so this option is disabled for Android 13+ until the bug is fixed
                // https://issuetracker.google.com/issues/237124750
                enabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                title = stringResource(R.string.appearance_blur_effect_title),
                summary = stringResource(
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                        R.string.appearance_blur_effect_summary
                    else
                        R.string.appearance_blur_effect_disabled_summary
                ),
                checked = viewModel.isBlurEffectEnabled,
                onCheckedChange = { toggleBlurEffect(it) }
            )

            SwitchSettingsListItem(
                icon = painterResource(R.drawable.ic_apps),
                title = stringResource(R.string.appearance_is_app_drawer_enabled_title),
                summary = stringResource(R.string.appearance_is_app_drawer_enabled_summary),
                checked = viewModel.isAppDrawerEnabled,
                onCheckedChange = { viewModel.changeIsAppDrawerEnabled(it) }
            )
        }
    }
}
