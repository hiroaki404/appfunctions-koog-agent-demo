package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
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
}
