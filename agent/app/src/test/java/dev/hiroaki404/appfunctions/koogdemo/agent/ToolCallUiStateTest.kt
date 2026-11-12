package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ToolCallUiStateTest {
    @Test
    fun startedThenCompletedUpdatesTheSameCall() {
        val state = ToolCallUiState()
            .apply(ToolCallUiEvent.Started("call-1", "convertCurrency", "{\"amount\":10}"))
            .apply(ToolCallUiEvent.Completed("call-1", "convertCurrency", "{}", "1000.0"))

        assertEquals(1, state.calls.size)
        assertEquals(ToolCallStatus.Succeeded, state.calls.single().status)
        assertEquals("1000.0", state.calls.single().result)
    }

    @Test
    fun nullIdCompletesLatestRunningCallForTheSameFunction() {
        val state = ToolCallUiState()
            .apply(ToolCallUiEvent.Started(null, "add", "{\"num1\":1}"))
            .apply(ToolCallUiEvent.Started(null, "add", "{\"num1\":2}"))
            .apply(ToolCallUiEvent.Completed(null, "add", "{}", "3"))

        assertEquals(ToolCallStatus.Running, state.calls[0].status)
        assertEquals(ToolCallStatus.Succeeded, state.calls[1].status)
        assertEquals("3", state.calls[1].result)
    }

    @Test
    fun failurePreservesChronologicalCalls() {
        val state = ToolCallUiState()
            .apply(ToolCallUiEvent.Started("first", "add", "{}"))
            .apply(ToolCallUiEvent.Started("second", "convertCurrency", "{}"))
            .apply(ToolCallUiEvent.Failed("second", "convertCurrency", "{}", "Unsupported currency"))

        assertEquals(listOf("add", "convertCurrency"), state.calls.map { it.functionName })
        assertEquals(ToolCallStatus.Running, state.calls[0].status)
        assertEquals(ToolCallStatus.Failed, state.calls[1].status)
        assertEquals("Unsupported currency", state.calls[1].error)
    }

    @Test
    fun awaitingApprovalMarksOnlyTheLatestRunningCallOfTheFunction() {
        val state = ToolCallUiState()
            .apply(ToolCallUiEvent.Started(null, "bookFlight", "{}"))
            .apply(ToolCallUiEvent.Completed(null, "bookFlight", "{}", "done"))
            .apply(ToolCallUiEvent.Started(null, "bookFlight", "{}"))
            .apply(ToolCallUiEvent.Started(null, "sendMessage", "{}"))
            .withAwaitingApproval("bookFlight", true)

        assertEquals(listOf(false, true, false), state.calls.map { it.awaitingApproval })
        assertEquals(ToolCallStatus.Running, state.calls[1].status)
    }

    @Test
    fun clearingAwaitingApprovalKeepsTheCallRunning() {
        val state = ToolCallUiState()
            .apply(ToolCallUiEvent.Started(null, "bookFlight", "{}"))
            .withAwaitingApproval("bookFlight", true)
            .withAwaitingApproval("bookFlight", false)

        assertFalse(state.calls.single().awaitingApproval)
        assertEquals(ToolCallStatus.Running, state.calls.single().status)
    }

    @Test
    fun awaitingApprovalForAnUnknownFunctionChangesNothing() {
        val state = ToolCallUiState().apply(ToolCallUiEvent.Started(null, "add", "{}"))

        assertEquals(state, state.withAwaitingApproval("bookFlight", true))
    }

    @Test
    fun finishingACallClearsAwaitingApproval() {
        val declined = ToolCallUiState()
            .apply(ToolCallUiEvent.Started(null, "bookFlight", "{}"))
            .withAwaitingApproval("bookFlight", true)
            .apply(ToolCallUiEvent.Completed(null, "bookFlight", "{}", "Error: The user declined this action."))
        val failed = ToolCallUiState()
            .apply(ToolCallUiEvent.Started("a", "bookFlight", "{}"))
            .withAwaitingApproval("bookFlight", true)
            .apply(ToolCallUiEvent.Failed("a", "bookFlight", "{}", "boom"))

        assertFalse(declined.calls.single().awaitingApproval)
        assertFalse(failed.calls.single().awaitingApproval)
    }
}
