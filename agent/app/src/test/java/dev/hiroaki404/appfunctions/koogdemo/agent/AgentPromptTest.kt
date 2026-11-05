package dev.hiroaki404.appfunctions.koogdemo.agent

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPromptTest {
    @Test
    fun containsTodayInIsoFormat() {
        val prompt = buildSystemPrompt(LocalDate.of(2026, 10, 25))

        assertTrue(prompt.contains("2026-10-25"))
    }

    @Test
    fun differentDatesProduceDifferentPrompts() {
        assertFalse(buildSystemPrompt(LocalDate.of(2026, 1, 1)) == buildSystemPrompt(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun instructsToAnswerInJapaneseAndAdmitMissingTools() {
        val prompt = buildSystemPrompt(LocalDate.of(2026, 10, 25))

        assertTrue(prompt.contains("日本語"))
        assertTrue(prompt.contains("ツール"))
        assertTrue(prompt.contains("できない"))
    }

    @Test
    fun includesAppInstructionsUnderTheirAppLabels() {
        val prompt = buildSystemPrompt(
            LocalDate.of(2026, 10, 25),
            listOf(AppInstructions("アプリA", "Call a before b."), AppInstructions("アプリB", "Limit: 5.")),
        )

        assertTrue(prompt.contains("### アプリA\nCall a before b."))
        assertTrue(prompt.contains("### アプリB\nLimit: 5."))
    }

    @Test
    fun omitsAppInstructionsSectionWhenThereAreNone() {
        assertFalse(buildSystemPrompt(LocalDate.of(2026, 10, 25)).contains("各アプリの説明"))
    }
}
