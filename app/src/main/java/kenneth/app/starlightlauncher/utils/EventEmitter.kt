package kenneth.app.starlightlauncher.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

typealias EventListener<TEvent> = (event: TEvent) -> Unit

/**
 * A class that can emit different events.
 */
abstract class EventEmitter<TEvent> {
    private val eventFlow = MutableSharedFlow<TEvent>()

    private val emitCoroutineScope = CoroutineScope(Dispatchers.Default)

    suspend fun listen(listener: EventListener<TEvent>) {
        eventFlow.collect { listener(it) }
    }

    protected fun emit(event: TEvent) {
        emitCoroutineScope.launch { eventFlow.emit(event) }
    }
}