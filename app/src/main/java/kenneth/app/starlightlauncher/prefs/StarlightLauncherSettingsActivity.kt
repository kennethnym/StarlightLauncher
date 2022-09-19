package kenneth.app.starlightlauncher.prefs

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.LauncherTheme
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.appearance.AppearanceSettingsScreen
import kenneth.app.starlightlauncher.prefs.appearance.IconPackSettingsScreen
import javax.inject.Inject

/**
 * The settings page for this launcher.
 */
@AndroidEntryPoint
class StarlightLauncherSettingsActivity : ComponentActivity() {
    @Inject
    internal lateinit var extensionManager: ExtensionManager

    override fun onStart() {
        super.onStart()
        setContent {
            val navController = rememberNavController()

            LauncherTheme {
                NavHost(navController, startDestination = SETTINGS_ROUTE_ROOT) {
                    composable(SETTINGS_ROUTE_ROOT) { RootSettingsScreen(navController) }
                    composable(SETTINGS_ROUTE_APPEARANCE) { AppearanceSettingsScreen(navController) }
                    composable(SETTINGS_ROUTE_ICON_PACK) { IconPackSettingsScreen() }
                }
            }
        }
    }
}
