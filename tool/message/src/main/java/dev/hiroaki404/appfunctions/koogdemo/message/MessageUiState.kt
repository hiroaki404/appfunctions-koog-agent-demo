package dev.hiroaki404.appfunctions.koogdemo.message

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The messages sent to one contact, oldest first. */
data class ContactConversation(
    val contact: Contact,
    val messages: List<SentMessage>,
)

data class MessageUiState(
    /** Conversations ordered by most recent activity first. Contacts never messaged are omitted. */
    val conversations: List<ContactConversation> = emptyList(),
) {
    /** The thread shown on screen: the contact of the most recent message. */
    val currentThread: ContactConversation? get() = conversations.firstOrNull()
}

fun List<SentMessage>.toUiState(): MessageUiState = MessageUiState(
    conversations = groupBy { it.contact }
        .map { (contact, messages) -> ContactConversation(contact, messages) }
        .sortedByDescending { conversation -> conversation.messages.last().sentAtMillis },
)

/** A row of a chat thread: either a day separator or a message. */
sealed interface ThreadItem {
    data class DateSeparator(val date: LocalDate) : ThreadItem
    data class Message(val message: SentMessage) : ThreadItem
}

/** Interleaves a [DateSeparator][ThreadItem.DateSeparator] before the first message of each day. */
fun List<SentMessage>.toThreadItems(zone: ZoneId = ZoneId.systemDefault()): List<ThreadItem> {
    val items = mutableListOf<ThreadItem>()
    var currentDate: LocalDate? = null
    for (message in this) {
        val date = Instant.ofEpochMilli(message.sentAtMillis).atZone(zone).toLocalDate()
        if (date != currentDate) {
            items += ThreadItem.DateSeparator(date)
            currentDate = date
        }
        items += ThreadItem.Message(message)
    }
    return items
}
