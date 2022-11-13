package kenneth.app.starlightlauncher.prefs.searching

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.component.SETTINGS_ROUTE_SEARCH_LAYOUT
import kenneth.app.starlightlauncher.prefs.component.SettingsList
import kenneth.app.starlightlauncher.prefs.component.SettingsListItem
import kenneth.app.starlightlauncher.prefs.component.SettingsScreen

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