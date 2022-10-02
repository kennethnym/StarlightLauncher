package kenneth.app.starlightlauncher.prefs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R

@Composable
fun RootSettingsScreen(
    navController: NavController
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
                icon = painterResource(R.drawable.ic_file_search_alt),
                title = stringResource(R.string.pref_search_title),
                summary = stringResource(R.string.pref_search_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_SEARCH) }
            )

            SettingsListItem(
                icon = painterResource(R.drawable.ic_info_circle),
                title = stringResource(R.string.launcher_info_title),
                summary = stringResource(R.string.launcher_info_summary),
                onTap = { navController.navigate(SETTINGS_ROUTE_INFO) }
            )
        }
    }
}