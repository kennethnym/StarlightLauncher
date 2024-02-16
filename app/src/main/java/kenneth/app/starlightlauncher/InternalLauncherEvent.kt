package kenneth.app.starlightlauncher

import kenneth.app.starlightlauncher.api.LauncherEvent

/**
 * Internal launcher events emitted by [LauncherEventChannel]
 * that are not intended to be consumed by plugins.
 */
internal abstract class InternalLauncherEvent : LauncherEvent()

internal class NightModeChanged(val isNightModeActive: Boolean) : InternalLauncherEvent()
