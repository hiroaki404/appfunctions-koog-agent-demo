package dev.hiroaki404.appfunctions.koogdemo.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MessageUiStateTest {
    private val tom = demoContacts[0]
    private val hanako = demoContacts[1]

    @Test
    fun emptyHistoryHasNoConversations() {
        assertTrue(emptyList<SentMessage>().toUiState().conversations.isEmpty())
    }

    @Test
    fun groupsMessagesPerContactWithMostRecentConversationFirst() {
        val history = listOf(
            SentMessage(tom, "first", 1),
            SentMessage(hanako, "second", 2),
            SentMessage(tom, "third", 3),
            SentMessage(hanako, "fourth", 4),
        )

        val conversations = history.toUiState().conversations

        assertEquals(listOf("花子", "トム"), conversations.map { it.contact.name })
        assertEquals(listOf("first", "third"), conversations.last().messages.map { it.text })
    }

    @Test
    fun repositoryRecordsMessagesInOrder() {
        var now = 100L
        val repository = MessageRepository(clock = { now++ })

        repository.record(tom, "a")
        repository.record(hanako, "b")

        assertEquals(listOf("a", "b"), repository.history.value.map { it.text })
        assertEquals(listOf(100L, 101L), repository.history.value.map { it.sentAtMillis })
    }

    @Test
    fun currentThreadIsContactOfMostRecentMessage() {
        val history = listOf(SentMessage(tom, "a", 1), SentMessage(hanako, "b", 2))

        assertEquals(hanako, history.toUiState().currentThread?.contact)
        assertNull(MessageUiState().currentThread)
    }

    @Test
    fun threadItemsInsertDateSeparatorWhenDayChanges() {
        val dayMillis = 24 * 60 * 60 * 1000L
        val messages = listOf(
            SentMessage(tom, "a", 1_000),
            SentMessage(tom, "b", 2_000),
            SentMessage(tom, "c", dayMillis + 1_000),
        )

        val items = messages.toThreadItems(ZoneOffset.UTC)

        assertEquals(
            listOf(
                ThreadItem.DateSeparator(LocalDate.of(1970, 1, 1)),
                ThreadItem.Message(messages[0]),
                ThreadItem.Message(messages[1]),
                ThreadItem.DateSeparator(LocalDate.of(1970, 1, 2)),
                ThreadItem.Message(messages[2]),
            ),
            items,
        )
    }
}
