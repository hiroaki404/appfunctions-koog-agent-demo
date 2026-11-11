package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBandTest {
    private fun group(packageName: String, label: String, isSystem: Boolean) =
        AvailableToolsGroup(packageName, ProviderApp(label, isSystem), functions = emptyList(), hasNew = false)

    private val groups = listOf(
        group("trip", "旅行", isSystem = false),
        group("message", "メッセージ", isSystem = false),
        group("settings", "設定", isSystem = true),
        group("launcher", "ランチャー", isSystem = true),
    )

    private fun running(packageName: String?, displayName: String? = null) = ToolCallUiModel(
        toolCallId = null,
        functionName = "searchItineraries",
        arguments = "{}",
        status = ToolCallStatus.Running,
        packageName = packageName,
        displayName = displayName,
    )

    @Test
    fun installedAppsAreListedAndSystemAppsAreCounted() {
        val state = appBandState(groups, running = null)

        assertEquals(listOf("trip", "message"), state.apps.map { it.packageName })
        assertEquals(2, state.systemAppCount)
        assertFalse(state.isCalling)
        assertTrue(state.apps.all { it.runningLabel == null })
    }

    @Test
    fun theRunningAppShowsTheFunctionDisplayName() {
        val state = appBandState(groups, running("trip", displayName = "旅程の検索"))

        assertEquals("旅程の検索", state.apps.first { it.packageName == "trip" }.runningLabel)
        assertNull(state.apps.first { it.packageName == "message" }.runningLabel)
        assertNull(state.systemRunningLabel)
        assertTrue(state.isCalling)
    }

    @Test
    fun aRunningSystemFunctionLightsTheSystemChipWithItsNameAsFallback() {
        val state = appBandState(groups, running("settings"))

        assertEquals("searchItineraries", state.systemRunningLabel)
        assertTrue(state.apps.all { it.runningLabel == null })
    }
}
