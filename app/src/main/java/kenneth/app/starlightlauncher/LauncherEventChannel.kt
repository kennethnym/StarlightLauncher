package kenneth.app.starlightlauncher

import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.LauncherEventListener
import kenneth.app.starlightlauncher.api.util.EventChannel
import kenneth.app.starlightlauncher.api.util.EventSubscriber
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherEventChannel @Inject constructor() : EventChannel<LauncherEvent>() {
    suspend fun subscribePublic(subscriber: LauncherEventListener) =
        eventFlow
            .filterNot { it is InternalLauncherEvent }
            .collect { subscriber(it) }
}