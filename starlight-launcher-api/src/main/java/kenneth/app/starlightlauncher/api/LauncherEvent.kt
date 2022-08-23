package kenneth.app.starlightlauncher.api

import android.content.pm.LauncherActivityInfo

/**
 * Defines different types of events emitted by Starlight Launcher.
 *
 * Use [StarlightLauncherApi.addLauncherEventListener] to subscribe to the events.
 */
abstract class LauncherEvent {
    /**
     * Emitted when user has changed the icon pack.
     */
    object IconPackChanged : LauncherEvent()

    data class NewAppsInstalled(val apps: List<LauncherActivityInfo>) : LauncherEvent()

    data class AppRemoved(val packageName: String) : LauncherEvent()

    data class AppsChanged(val apps: List<LauncherActivityInfo>) : LauncherEvent()
}
