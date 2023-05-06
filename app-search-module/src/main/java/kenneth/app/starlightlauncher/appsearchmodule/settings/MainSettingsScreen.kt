package kenneth.app.starlightlauncher.appsearchmodule.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kenneth.app.starlightlauncher.api.compose.LocalDataStore
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.api.compose.pref.SwitchSettingsListItem
import kenneth.app.starlightlauncher.appsearchmodule.R

@Composable
fun MainSettingsScreen() {
    val dataStore = LocalDataStore.current
    val viewModel: MainSettingsScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainSettingsScreenViewModel(dataStore) as T
            }
        }
    )

    SettingsScreen(
        title = stringResource(R.string.app_search_module_settings_title),
        description = stringResource(R.string.app_search_module_settings_screen_description)
    ) {
        SettingsList {
            SwitchSettingsListItem(
                title = stringResource(R.string.app_search_module_settings_show_app_labels),
                checked = viewModel.shouldShowAppNames,
                emptyIcon = false,
                onCheckedChange = {
                    viewModel.setAppNamesVisibility(isVisible = it)
                }
            )

            SwitchSettingsListItem(
                title = stringResource(R.string.app_search_module_settings_show_pinned_app_labels),
                checked = viewModel.shouldShowPinnedAppNames,
                emptyIcon = false,
                onCheckedChange = {
                    viewModel.setPinnedAppNamesVisibility(isVisible = it)
                }
            )
        }
    }
}