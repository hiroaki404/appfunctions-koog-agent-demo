package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTurnTest {
    @Test
    fun newTurnIsRunningWithoutResult() {
        val turn = ChatTurn(id = 1, prompt = "hello")

        assertTrue(turn.isRunning)
        assertNull(turn.response)
        assertNull(turn.error)
    }

    @Test
    fun toolEventsAccumulateOnTheTurn() {
        val turn = ChatTurn(id = 1, prompt = "hello")
            .applyToolEvent(ToolCallUiEvent.Started("c1", "searchTrips", "{}"))
            .applyToolEvent(ToolCallUiEvent.Completed("c1", "searchTrips", "{}", "[]"))

        assertEquals(1, turn.toolCalls.calls.size)
        assertEquals(ToolCallStatus.Succeeded, turn.toolCalls.calls.single().status)
    }

    @Test
    fun thinkingOnlyWhileWaitingOnTheLlm() {
        val started = ChatTurn(id = 1, prompt = "hello")
        val toolRunning = started.applyToolEvent(ToolCallUiEvent.Started("c1", "searchTrips", "{}"))
        val toolDone = toolRunning.applyToolEvent(ToolCallUiEvent.Completed("c1", "searchTrips", "{}", "[]"))

        assertTrue(started.isThinking)
        assertFalse(toolRunning.isThinking)
        assertTrue(toolDone.isThinking)
        assertFalse(toolDone.completed("answer").isThinking)
        assertFalse(toolDone.failed("boom").isThinking)
    }

    @Test
    fun completedStopsRunningAndStoresResponse() {
        val turn = ChatTurn(id = 1, prompt = "hello").completed("answer")

        assertFalse(turn.isRunning)
        assertEquals("answer", turn.response)
    }

    @Test
    fun failedStopsRunningAndStoresError() {
        val turn = ChatTurn(id = 1, prompt = "hello").failed("boom")

        assertFalse(turn.isRunning)
        assertEquals("boom", turn.error)
        assertNull(turn.response)
    }

    @Test
    fun updateTurnOnlyTouchesMatchingTurn() {
        val turns = listOf(ChatTurn(id = 1, prompt = "a"), ChatTurn(id = 2, prompt = "b"))
            .updateTurn(2) { it.completed("done") }

        assertTrue(turns[0].isRunning)
        assertEquals("done", turns[1].response)
    }

    @Test
    fun updateTurnIgnoresMissingTurn() {
        val turns = listOf(ChatTurn(id = 1, prompt = "a"))

        assertEquals(turns, turns.updateTurn(99) { it.completed("done") })
    }
}
