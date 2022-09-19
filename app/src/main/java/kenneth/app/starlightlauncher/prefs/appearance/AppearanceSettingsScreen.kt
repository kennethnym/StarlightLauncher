package kenneth.app.starlightlauncher.prefs.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.SETTINGS_ROUTE_ICON_PACK
import kenneth.app.starlightlauncher.prefs.SettingsListItem
import kenneth.app.starlightlauncher.prefs.SettingsScreen

@Composable
fun AppearanceSettingsScreen(
    navController: NavController
) {
    SettingsScreen(
        title = stringResource(R.string.appearance_title),
        description = stringResource(R.string.appearance_subtitle),
        gutter = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsListItem(
                icon = painterResource(R.drawable.ic_icons),
                title = stringResource(R.string.appearance_change_icon_pack_title),
                summary = stringResource(R.string.appearance_change_icon_pack_summary)
            ) {
                navController.navigate(SETTINGS_ROUTE_ICON_PACK)
            }
        }
    }
}
