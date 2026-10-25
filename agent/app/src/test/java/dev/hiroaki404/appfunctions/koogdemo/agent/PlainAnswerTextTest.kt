package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainAnswerTextTest {
    @Test
    fun stripsBoldMarkers() {
        assertEquals("日程は 12月 です", plainAnswerText("日程は **12月** です"))
    }

    @Test
    fun convertsLeadingBullets() {
        val markdown = "候補:\n* 小樽\n  * 運河\n* 函館"

        assertEquals("候補:\n・小樽\n  ・運河\n・函館", plainAnswerText(markdown))
    }

    @Test
    fun handlesBoldInsideBullets() {
        assertEquals("・小樽: 運河", plainAnswerText("* **小樽**: 運河"))
    }

    @Test
    fun keepsInlineAsterisks() {
        assertEquals("2 * 3 = 6", plainAnswerText("2 * 3 = 6"))
    }
}
