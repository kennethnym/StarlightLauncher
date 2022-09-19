package kenneth.app.starlightlauncher.prefs

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

@Composable
fun RootSettingsScreen(
    navController: NavController
) {
    SettingsScreen(
        title = stringResource(R.string.settings_title),
        description = stringResource(R.string.settings_subtitle),
        gutter = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsListItem(
                icon = painterResource(R.drawable.ic_swatchbook),
                title = stringResource(R.string.appearance_title),
                summary = stringResource(R.string.appearance_summary)
            ) { navController.navigate(SETTINGS_ROUTE_APPEARANCE) }

            SettingsListItem(
                icon = painterResource(R.drawable.ic_file_search_alt),
                title = stringResource(R.string.pref_search_title),
                summary = stringResource(R.string.pref_search_summary)
            )

            SettingsListItem(
                icon = painterResource(R.drawable.ic_info_circle),
                title = stringResource(R.string.pref_info_title),
                summary = stringResource(R.string.pref_info_summary)
            )
        }

    }
}