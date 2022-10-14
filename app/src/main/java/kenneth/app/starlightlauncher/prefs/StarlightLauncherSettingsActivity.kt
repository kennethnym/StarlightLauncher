package kenneth.app.starlightlauncher.prefs

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.LauncherTheme
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.appearance.AppearanceSettingsScreen
import kenneth.app.starlightlauncher.prefs.appearance.IconPackSettingsScreen
import kenneth.app.starlightlauncher.prefs.component.*
import kenneth.app.starlightlauncher.datetime.ClockSettingsScreen
import kenneth.app.starlightlauncher.prefs.searching.SearchLayoutSettingsScreen
import kenneth.app.starlightlauncher.prefs.searching.SearchSettingsScreen
import javax.inject.Inject

/**
 * The settings page for this launcher.
 */
@AndroidEntryPoint
class StarlightLauncherSettingsActivity : ComponentActivity() {
    // Tree layout of the settings screens
    //
    // RootSettingsScreen                     Route name
    // ├── AppearanceSettingsScreen           (appearance)
    // │   ├── Blur effect
    // │   └── Icon pack
    // │   │   └── IconPackSettingsScreen     (appearance/icon_pack)
    // │   └── Clock settings
    // │       └── ClockSettingsScreen        (appearance/clock)
    // ├── SearchSettingsScreen               (search)
    // │   ├── Search layout
    // │   │   └── SearchLayoutSettingsScreen (search/layout)
    // │   └── Search engine
    // │       └── SearchEngineSettingsScreen
    // └── Info
    //     └── InfoSettingsScreen              (info)

    @Inject
    internal lateinit var extensionManager: ExtensionManager

    @OptIn(ExperimentalMaterialApi::class)
    override fun onStart() {
        super.onStart()
        setContent {
            val navController = rememberNavController()

            LauncherTheme {
                NavHost(navController, startDestination = SETTINGS_ROUTE_ROOT) {
                    composable(SETTINGS_ROUTE_ROOT) { RootSettingsScreen(navController) }
                    composable(SETTINGS_ROUTE_APPEARANCE) { AppearanceSettingsScreen(navController) }
                    composable(SETTINGS_ROUTE_CLOCK) { ClockSettingsScreen() }
                    composable(SETTINGS_ROUTE_ICON_PACK) { IconPackSettingsScreen() }

                    composable(SETTINGS_ROUTE_SEARCH) { SearchSettingsScreen(navController) }
                    composable(SETTINGS_ROUTE_SEARCH_LAYOUT) { SearchLayoutSettingsScreen() }

                    composable(SETTINGS_ROUTE_INFO) { InfoSettingsScreen() }
                }
            }
        }
    }
}
