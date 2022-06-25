package kenneth.app.starlightlauncher

import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.LauncherEventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An event channel for adding/subscribing to launcher events.
 */
@Singleton
internal class LauncherEventChannel @Inject constructor(
    defaultDispatcher: CoroutineDispatcher
) {
    private val eventFlow = MutableSharedFlow<LauncherEvent>()

    private val addEventCoroutineScope = CoroutineScope(defaultDispatcher)

    /**
     * Subscribes to events emitted by Starlight Launcher.
     * [subscriber] will be notified whenever a new event is emitted.
     */
    suspend fun subscribe(subscriber: LauncherEventListener) =
        eventFlow.collect { subscriber(it) }

    /**
     * Adds [event] to the channel. Subscribers of the channel will be notified of the event.
     */
    fun add(event: LauncherEvent) {
        addEventCoroutineScope.launch { eventFlow.emit(event) }
    }
}