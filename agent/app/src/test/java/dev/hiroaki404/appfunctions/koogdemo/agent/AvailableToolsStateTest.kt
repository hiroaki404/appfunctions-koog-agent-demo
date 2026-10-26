package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvailableToolsStateTest {
    private val bookFlight = AvailableFunction("travel.Base#bookFlight", "travel", "Book a flight.")
    private val sendMessage = AvailableFunction("message.Base#sendMessage", "message", "Send a message.")
    private val battery = AvailableFunction("settings.Base#getBatteryDeviceState", "settings", "")

    @Test
    fun firstResultIsTheBaselineWithNothingNew() {
        val state = AvailableToolsState().update(listOf(bookFlight, battery))

        assertEquals(2, state.functions?.size)
        assertEquals(emptySet<String>(), state.newFunctionIds)
    }

    @Test
    fun functionsAddedLaterAreNewUntilAcknowledged() {
        val state = AvailableToolsState()
            .update(listOf(bookFlight, battery))
            .update(listOf(bookFlight, battery, sendMessage))

        assertEquals(setOf(sendMessage.id), state.newFunctionIds)
        assertEquals(emptySet<String>(), state.acknowledged().newFunctionIds)
    }

    @Test
    fun removedFunctionsStopCountingAsNew() {
        val state = AvailableToolsState()
            .update(listOf(bookFlight))
            .update(listOf(bookFlight, sendMessage))
            .update(listOf(bookFlight))

        assertEquals(emptySet<String>(), state.newFunctionIds)
    }

    @Test
    fun noFunctionsBeforeTheFirstSearch() {
        assertNull(AvailableToolsState().functions)
    }

    @Test
    fun groupsPutUserAppsBeforeSystemAppsAndMarkNewOnes() {
        val apps = mapOf(
            "settings" to ProviderApp("設定", isSystem = true),
            "travel" to ProviderApp("kototabi", isSystem = false),
            "message" to ProviderApp("メッセージ", isSystem = false),
        )
        val groups = AvailableToolsState()
            .update(listOf(battery, bookFlight))
            .update(listOf(battery, bookFlight, sendMessage))
            .groups { apps.getValue(it) }

        assertEquals(listOf("travel", "message", "settings"), groups.map { it.packageName })
        assertEquals(listOf(false, true, false), groups.map { it.hasNew })
    }

    @Test
    fun nameDropsTheDeclaringClass() {
        assertEquals("bookFlight", bookFlight.name)
    }
}
