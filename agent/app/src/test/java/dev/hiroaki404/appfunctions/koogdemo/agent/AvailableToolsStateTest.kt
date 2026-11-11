package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            "travel" to ProviderApp("旅行", isSystem = false),
            "message" to ProviderApp("メッセージ", isSystem = false),
        )
        val groups = AvailableToolsState()
            .update(listOf(battery, bookFlight))
            .update(listOf(battery, bookFlight, sendMessage))
            .groups { apps.getValue(it) }

        assertEquals(listOf("message", "travel", "settings"), groups.map { it.packageName })
        assertEquals(listOf(true, false, false), groups.map { it.hasNew })
    }

    @Test
    fun aNewAppBecomesADiscoveryWithItsFunctions() {
        val state = AvailableToolsState()
            .update(listOf(bookFlight, battery))
            .update(listOf(bookFlight, battery, sendMessage))

        assertEquals(setOf("message"), state.discovery?.packageNames)
        assertEquals(listOf(sendMessage), state.discovery?.functions)
    }

    @Test
    fun theFirstResultAndMoreFunctionsFromAKnownAppAreNoDiscovery() {
        val searchTrips = AvailableFunction("travel.Base#searchTrips", "travel", "")
        val state = AvailableToolsState().update(listOf(bookFlight))

        assertNull(state.discovery)
        assertNull(state.update(listOf(bookFlight, searchTrips)).discovery)
    }

    @Test
    fun everyFindGetsANewIdEvenAfterTheLastWasCleared() {
        val first = AvailableToolsState()
            .update(listOf(bookFlight))
            .update(listOf(bookFlight, sendMessage))
        val second = first.acknowledged()
            .update(listOf(bookFlight))
            .update(listOf(bookFlight, sendMessage))

        assertNull(first.acknowledged().discovery)
        assertTrue(second.discovery!!.id > first.discovery!!.id)
    }

    @Test
    fun uninstallingAFoundAppDropsItFromTheDiscovery() {
        val state = AvailableToolsState()
            .update(listOf(bookFlight))
            .update(listOf(bookFlight, sendMessage))
            .update(listOf(bookFlight))

        assertNull(state.discovery)
    }

    @Test
    fun discoveredAppsTextAddsアプリOnlyWhenMissing() {
        assertEquals("メッセージアプリを見つけました", discoveredAppsText(listOf("メッセージ")))
        assertEquals("旅行アプリを見つけました", discoveredAppsText(listOf("旅行アプリ")))
        assertEquals("2個のアプリを見つけました", discoveredAppsText(listOf("メッセージ", "旅行")))
    }

    @Test
    fun discoveredFunctionsTextPrefersDisplayNames() {
        val named = sendMessage.copy(displayName = "メッセージの送信")

        assertEquals("メッセージの送信・bookFlight", discoveredFunctionsText(listOf(named, bookFlight)))
    }

    @Test
    fun nameDropsTheDeclaringClass() {
        assertEquals("bookFlight", bookFlight.name)
    }
}
