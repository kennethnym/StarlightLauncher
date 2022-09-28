package kenneth.app.starlightlauncher.prefs.appearance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.SETTINGS_ROUTE_ICON_PACK
import kenneth.app.starlightlauncher.prefs.SettingsListItem
import kenneth.app.starlightlauncher.prefs.SettingsScreen

@Composable
internal fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: AppearanceSettingsScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permissionRequestLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.setIsBlurEffectEnabled(isGranted)
        }

    fun toggleBlurEffect(isEnabled: Boolean) {
        val hasPermission =
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (isEnabled && !hasPermission) {
            permissionRequestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            viewModel.setIsBlurEffectEnabled(isEnabled)
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

            SettingsListItem(
                icon = null,
                title = stringResource(R.string.appearance_blur_effect_title),
                summary = stringResource(R.string.appearance_blur_effect_summary),
                control = {
                    Switch(
                        checked = viewModel.isBlurEffectEnabled,
                        onCheckedChange = { toggleBlurEffect(it) }
                    )
                },
                onTap = {
                    toggleBlurEffect(!viewModel.isBlurEffectEnabled)
                }
            )
        }
    }
}
