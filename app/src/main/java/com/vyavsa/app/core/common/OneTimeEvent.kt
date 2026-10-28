package com.vyavsa.app.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Encapsulates a transient UI message or event that should only be consumed once.
 * Even if the screen is recomposed, restored from backstack, or reconstructed on configuration change,
 * an event that has already been consumed will not be processed again.
 */
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType = MessageType.INFO
)

enum class MessageType {
    INFO,
    SUCCESS,
    ERROR
}

/**
 * Manages one-time UI events backed by a buffered Channel.
 * Guarantees that each event is received exactly once by active collectors.
 */
class OneTimeEventEmitter {
    private val _events = Channel<UiMessage>(capacity = Channel.BUFFERED)
    val events: Flow<UiMessage> = _events.receiveAsFlow()

    fun emit(message: String, type: MessageType = MessageType.INFO) {
        _events.trySend(UiMessage(text = message, type = type))
    }

    suspend fun emitSuspend(message: String, type: MessageType = MessageType.INFO) {
        _events.send(UiMessage(text = message, type = type))
    }
}

/**
 * Collects one-time events safely when the lifecycle is at least STARTED.
 * Ensures consumed events are never replayed upon tab switching, backstack navigation,
 * or recomposition.
 */
@Composable
fun ObserveAsEvents(
    flow: Flow<UiMessage>,
    onEvent: suspend (UiMessage) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect { event ->
                    onEvent(event)
                }
            }
        }
    }
}
