package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatDisplayTest {
    @Test
    fun kototabiOwnerGetsKototabiLabel() {
        val name = toolDisplayNameOf("BaseKototabiAppFunctionService_bookFlight")

        assertEquals("bookFlight", name.function)
        assertEquals("kototabi", name.appLabel)
    }

    @Test
    fun messageOwnerGetsJapaneseLabel() {
        val name = toolDisplayNameOf("BaseMessageAppFunctionService_sendMessage")

        assertEquals("sendMessage", name.function)
        assertEquals("メッセージ", name.appLabel)
    }

    @Test
    fun unknownOwnerStripsBasePrefixAndServiceSuffix() {
        val name = toolDisplayNameOf("BaseCalendarAppFunctionService_addEvent")

        assertEquals("addEvent", name.function)
        assertEquals("Calendar", name.appLabel)
    }

    @Test
    fun functionNameKeepsLaterUnderscores() {
        assertEquals("book_flight", toolDisplayNameOf("BaseKototabiAppFunctionService_book_flight").function)
    }

    @Test
    fun nameWithoutOwnerHasNoLabel() {
        val name = toolDisplayNameOf("convertCurrency")

        assertEquals("convertCurrency", name.function)
        assertNull(name.appLabel)
    }

    @Test
    fun plainAnswerTextStripsBoldMarkers() {
        assertEquals("日程は 12月 です", plainAnswerText("日程は **12月** です"))
    }

    @Test
    fun plainAnswerTextConvertsLeadingBullets() {
        val markdown = "候補:\n* 小樽\n  * 運河\n* 函館"

        assertEquals("候補:\n・小樽\n  ・運河\n・函館", plainAnswerText(markdown))
    }

    @Test
    fun plainAnswerTextHandlesBoldInsideBullets() {
        assertEquals("・小樽: 運河", plainAnswerText("* **小樽**: 運河"))
    }

    @Test
    fun plainAnswerTextKeepsInlineAsterisks() {
        assertEquals("2 * 3 = 6", plainAnswerText("2 * 3 = 6"))
    }
}
