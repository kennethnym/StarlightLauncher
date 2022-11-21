package kenneth.app.starlightlauncher.prefs.searching

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.prefs.SETTINGS_ROUTE_SEARCH_LAYOUT

@Composable
internal fun SearchSettingsScreen(
    navController: NavController
) {
    SettingsScreen(
        title = stringResource(R.string.pref_search_title),
        description = stringResource(id = R.string.pref_search_subtitle)
    ) {
        SettingsList {
            SettingsListItem(
                title = stringResource(R.string.pref_search_layout_title),
                summary = stringResource(R.string.pref_search_layout_subtitle),
                icon = painterResource(R.drawable.ic_window_grid),
                onTap = { navController.navigate(SETTINGS_ROUTE_SEARCH_LAYOUT) }
            )
        }
    }
}
