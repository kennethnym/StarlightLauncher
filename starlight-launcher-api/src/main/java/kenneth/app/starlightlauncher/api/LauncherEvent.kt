package kenneth.app.starlightlauncher.api

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
}
