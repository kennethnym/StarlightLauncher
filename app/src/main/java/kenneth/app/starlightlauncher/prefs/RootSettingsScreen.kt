package kenneth.app.starlightlauncher.prefs

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen

@Composable
internal fun RootSettingsScreen(
    navController: NavController,
    viewModel: RootSettingsScreenViewModel = hiltViewModel()
) {
    SettingsScreen(
        title = stringResource(R.string.settings_title),
        description = stringResource(R.string.settings_subtitle),
        gutter = true
    ) {
        SettingsList {
            SettingsListItem(
                icon = painterResource(R.drawable.ic_swatchbook),
                title = stringResource(R.string.appearance_title),
                summary = stringResource(R.string.appearance_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_APPEARANCE) }
            )

            SettingsListItem(
                icon = painterResource(R.drawable.ic_clock),
                title = stringResource(R.string.pref_clock_settings_title),
                summary = stringResource(R.string.pref_clock_settings_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_CLOCK) }
            )

            SettingsListItem(
                icon = painterResource(R.drawable.ic_file_search_alt),
                title = stringResource(R.string.pref_search_title),
                summary = stringResource(R.string.pref_search_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_SEARCH) }
            )

            Divider()

            viewModel.installedExtensions.forEach { ext ->
                ext.settingsProvider?.let {
                    SettingsListItem(
                        icon = painterResource(it.settingsIconRes),
                        title = it.settingsTitle,
                        summary = it.settingsSummary,
                        onTap = { navController.navigate(rootExtensionRoute(ext.name)) }
                    )
                }
            }

            Divider()

            SettingsListItem(
                icon = painterResource(R.drawable.ic_info_circle),
                title = stringResource(R.string.launcher_info_title),
                summary = stringResource(R.string.launcher_info_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_INFO) }
            )
        }
    }
}