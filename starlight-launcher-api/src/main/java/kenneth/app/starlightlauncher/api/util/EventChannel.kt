package kenneth.app.starlightlauncher.api.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Defines a subscriber to [EventChannel].
 */
typealias EventSubscriber<TEvent> = (event: TEvent) -> Unit

/**
 * A channel for subscribing to events added externally through [EventChannel.add].
 */
open class EventChannel<TEvent>(
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    protected val eventFlow = MutableSharedFlow<TEvent>()

    private val addEventCoroutineScope = CoroutineScope(defaultDispatcher)

    /**
     * Subscribes to events emitted by Starlight Launcher.
     * [subscriber] will be notified whenever a new event is emitted.
     */
    suspend fun subscribe(subscriber: EventSubscriber<TEvent>) =
        eventFlow.collect { subscriber(it) }

    /**
     * Adds [event] to the channel. Subscribers of the channel will be notified of the event.
     */
    fun add(event: TEvent) {
        addEventCoroutineScope.launch { eventFlow.emit(event) }
    }
}
