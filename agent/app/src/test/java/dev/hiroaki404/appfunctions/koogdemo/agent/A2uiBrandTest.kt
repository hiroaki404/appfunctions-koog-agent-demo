package dev.hiroaki404.appfunctions.koogdemo.agent

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A2uiBrandTest {
    @Test
    fun readsThePrimaryColorOfEachCreatedSurface() {
        val json = """
            [
              {"version":"v0.9.1","createSurface":{"surfaceId":"a","catalogId":"c","theme":{"primaryColor":"#00778D"}}},
              {"version":"v0.9.1","createSurface":{"surfaceId":"b","catalogId":"c"}},
              {"version":"v0.9.1","updateComponents":{"surfaceId":"a","components":[]}}
            ]
        """.trimIndent()

        assertEquals(mapOf("a" to Color(0xFF00778D)), a2uiPrimaryColorsOf(json))
    }

    @Test
    fun malformedJsonGivesNoColors() {
        assertTrue(a2uiPrimaryColorsOf("not json").isEmpty())
    }

    @Test
    fun parsesSixAndEightDigitHexColorsOnly() {
        assertEquals(Color(0xFF00778D), parseHexColor("#00778D"))
        assertEquals(Color(0x8000778D), parseHexColor("#8000778D"))
        assertNull(parseHexColor("#0778"))
        assertNull(parseHexColor("blue"))
        assertNull(parseHexColor(null))
    }
}
