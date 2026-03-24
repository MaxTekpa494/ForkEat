package fr.uge.android.forkeat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PromotionEventBus {
    data class Event(val title: String, val body: String)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(event: Event) { _events.tryEmit(event) }
}
