package dev.hiroaki404.appfunctions.koogdemo.message

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A message recorded by the app. Nothing is actually delivered anywhere. */
data class SentMessage(
    val contact: Contact,
    val text: String,
    val sentAtMillis: Long,
)

/**
 * In-memory sent-message history, shared by the AppFunction service and the activity.
 *
 * Both run in the same process, so a [StateFlow] is enough; the history is intentionally lost
 * when the process dies.
 */
class MessageRepository(private val clock: () -> Long = System::currentTimeMillis) {
    private val _history = MutableStateFlow<List<SentMessage>>(emptyList())
    val history: StateFlow<List<SentMessage>> = _history.asStateFlow()

    fun record(contact: Contact, text: String): SentMessage {
        val message = SentMessage(contact, text, clock())
        _history.update { it + message }
        return message
    }
}
